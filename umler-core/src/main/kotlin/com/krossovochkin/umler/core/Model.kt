package com.krossovochkin.umler.core

import com.google.gson.Gson
import java.io.File

interface Element {
    val id: String
    val name: String
}

data class InterfaceElement(
    override val name: String,
    override val id: String = java.util.UUID.randomUUID().toString()
) : Element

data class ClassElement(
    override val name: String,
    override val id: String = java.util.UUID.randomUUID().toString()
) : Element

interface Connection {
    val start: Element
    val end: Element
}

data class ExtendsConnection(
    override val start: Element,
    override val end: Element
) : Connection

data class ImplementsConnection(
    override val start: Element,
    override val end: Element
) : Connection

data class AggregatesConnection(
    override val start: Element,
    override val end: Element
) : Connection

data class UsesConnection(
    override val start: Element,
    override val end: Element
): Connection

private val gson = Gson()

fun readUml(file: File): Pair<Set<Element>, Set<Connection>> {
    val umlData = gson.fromJson(file.readText(), UmlData::class.java)

    val elements = umlData.elements
        .map {
            when (val type = it.type) {
                ClassElement::class.java.canonicalName -> {
                    ClassElement(
                        id = it.id,
                        name = it.name
                    )
                }
                InterfaceElement::class.java.canonicalName -> {
                    InterfaceElement(
                        id = it.id,
                        name = it.name
                    )
                }
                else -> error("Unknown type: $type")
            }
        }
        .toSet()

    val elementsMap = elements.map { it.id to it }.toMap()
    val connections = umlData.connections
        .map {
            val start = elementsMap[it.startId]!!
            val end = elementsMap[it.endId]!!

            when (val type = it.type) {
                ExtendsConnection::class.java.canonicalName -> {
                    ExtendsConnection(
                        start = start,
                        end = end
                    )
                }
                ImplementsConnection::class.java.canonicalName -> {
                    ImplementsConnection(
                        start = start,
                        end = end
                    )
                }
                AggregatesConnection::class.java.canonicalName -> {
                    AggregatesConnection(
                        start = start,
                        end = end
                    )
                }
                UsesConnection::class.java.canonicalName -> {
                    UsesConnection(
                        start = start,
                        end = end
                    )
                }
                else -> error("Unknown type: $type")
            }
        }
        .toSet()

    return elements to connections
}

fun writeUml(
    elements: Set<Element>,
    connections: Set<Connection>,
    file: File
) {
    file.writeText(
        gson.toJson(
            UmlData(
                elements = elements
                    .map {
                        ElementData(
                            name = it.name,
                            id = it.id,
                            type = it::class.java.canonicalName
                        )
                    }
                    .toSet(),
                connections
                    .map {
                        ConnectionData(
                            startId = it.start.id,
                            endId = it.end.id,
                            type = it::class.java.canonicalName
                        )
                    }
                    .toSet()
            ),
            UmlData::class.java
        )
    )
}

private data class ElementData(
    val id: String,
    val name: String,
    val type: String
)

private data class ConnectionData(
    val startId: String,
    val endId: String,
    val type: String
)

private data class UmlData(
    val elements: Set<ElementData>,
    val connections: Set<ConnectionData>
)