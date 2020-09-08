package com.krossovochkin.umler.sample

interface FileProcessor {

    fun process(folder: Folder)
}

class FileCountProcessor : FileProcessor {

    override fun process(folder: Folder) {
        println(getFilesCount(folder))
    }

    private fun getFilesCount(folder: Folder): Int {
        var count = 0
        folder.entries.forEach {
            if (it is File) {
                count++
            } else if (it is Folder) {
                count += getFilesCount(it)
            }
        }
        return count
    }
}