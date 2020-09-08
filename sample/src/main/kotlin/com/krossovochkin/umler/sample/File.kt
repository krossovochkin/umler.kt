package com.krossovochkin.umler.sample

interface FileSystemEntry {

    val path: String
}

interface File : FileSystemEntry {

    val extension: String
}

interface Folder : FileSystemEntry {

    val entries: List<FileSystemEntry>
}

data class FileImpl(
    override val path: String,
    override val extension: String
): File

data class FolderImpl(
    override val path: String,
    override val entries: List<FileSystemEntry>
): Folder
