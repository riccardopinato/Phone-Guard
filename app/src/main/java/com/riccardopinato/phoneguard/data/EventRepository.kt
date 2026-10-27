package com.riccardopinato.phoneguard.data

import android.content.Context
import android.util.AtomicFile
import com.riccardopinato.phoneguard.model.AlarmEvent
import com.riccardopinato.phoneguard.model.AlarmEventType
import java.io.File
import kotlin.math.max
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.json.JSONArray
import org.json.JSONObject

class EventRepository(context: Context) {

    private val file =
        File(
            context.filesDir,
            "events.json"
        )

    private val atomicFile =
        AtomicFile(file)

    private val scope =
        CoroutineScope(
            SupervisorJob() +
                Dispatchers.IO
        )

    private val mutex =
        Mutex()

    private val _events =
        MutableStateFlow(
            loadFromDisk()
        )

    val events:
        StateFlow<List<AlarmEvent>> =
        _events.asStateFlow()

    fun add(
        type: AlarmEventType,
        detail: String = "",
        metadata: Map<String, String> =
            emptyMap()
    ) {
        scope.launch {
            mutex.withLock {
                val now =
                    System.currentTimeMillis()

                val event =
                    AlarmEvent(
                        id =
                            nextEventId(
                                now
                            ),
                        timestamp =
                            now,
                        type =
                            type,
                        detail =
                            detail,
                        metadata =
                            metadata
                    )

                val updated =
                    (
                        listOf(event) +
                            _events.value
                        )
                        .take(
                            MAX_EVENTS
                        )

                persist(updated)
                _events.value =
                    updated
            }
        }
    }

    fun clear() {
        scope.launch {
            mutex.withLock {
                persist(
                    emptyList()
                )

                _events.value =
                    emptyList()
            }
        }
    }

    private fun nextEventId(
        nowMillis: Long
    ): Long {
        val timeBased =
            nowMillis *
                ID_MULTIPLIER

        val lastId =
            _events.value
                .maxOfOrNull {
                    it.id
                }
                ?: 0L

        return max(
            timeBased,
            lastId + 1L
        )
    }

    private fun loadFromDisk():
        List<AlarmEvent> {

        if (!file.exists()) {
            return emptyList()
        }

        return runCatching {
            val raw =
                atomicFile
                    .openRead()
                    .bufferedReader(
                        Charsets.UTF_8
                    )
                    .use {
                        it.readText()
                    }

            val array =
                JSONArray(raw)

            buildList {
                for (
                    i in
                    0 until
                        array.length()
                ) {
                    val item =
                        array
                            .getJSONObject(
                                i
                            )

                    val metadata =
                        item
                            .optJSONObject(
                                "metadata"
                            )
                            ?.let {
                                obj ->
                                buildMap {
                                    val keys =
                                        obj.keys()

                                    while (
                                        keys.hasNext()
                                    ) {
                                        val key =
                                            keys.next()

                                        put(
                                            key,
                                            obj.optString(
                                                key
                                            )
                                        )
                                    }
                                }
                            }
                            ?: emptyMap()

                    add(
                        AlarmEvent(
                            id =
                                item
                                    .optLong(
                                        "id"
                                    ),
                            timestamp =
                                item
                                    .optLong(
                                        "timestamp"
                                    ),
                            type =
                                runCatching {
                                    AlarmEventType
                                        .valueOf(
                                            item
                                                .optString(
                                                    "type"
                                                )
                                        )
                                }
                                    .getOrDefault(
                                        AlarmEventType
                                            .ERROR
                                    ),
                            detail =
                                item
                                    .optString(
                                        "detail"
                                    ),
                            metadata =
                                metadata
                        )
                    )
                }
            }
        }
            .getOrDefault(
                emptyList()
            )
    }

    private fun persist(
        events: List<AlarmEvent>
    ) {
        val array =
            JSONArray()

        events.forEach {
            event ->

            val metadata =
                JSONObject()

            event.metadata.forEach {
                (key, value) ->
                metadata.put(
                    key,
                    value
                )
            }

            array.put(
                JSONObject()
                    .put(
                        "id",
                        event.id
                    )
                    .put(
                        "timestamp",
                        event.timestamp
                    )
                    .put(
                        "type",
                        event.type.name
                    )
                    .put(
                        "detail",
                        event.detail
                    )
                    .put(
                        "metadata",
                        metadata
                    )
            )
        }

        val output =
            atomicFile
                .startWrite()

        try {
            output.write(
                array
                    .toString()
                    .toByteArray(
                        Charsets.UTF_8
                    )
            )

            atomicFile
                .finishWrite(
                    output
                )
        } catch (
            throwable:
                Throwable
        ) {
            atomicFile
                .failWrite(
                    output
                )

            throw throwable
        }
    }

    private companion object {
        const val MAX_EVENTS =
            200

        const val ID_MULTIPLIER =
            1_000L
    }
}
