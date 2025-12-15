package com.ileveli.ksp

import com.google.devtools.ksp.processing.*
import com.google.devtools.ksp.symbol.*
import java.io.File
import java.nio.charset.Charset
import java.nio.file.Path
import kotlin.io.path.Path

/**
 * A KSP (Kotlin Symbol Processing) processor that generates a Kotlin object
 * containing `const val` declarations for message keys found in localized
 * `.properties` files.
 *
 * This processor helps in centralizing localization keys as compile-time constants,
 * reducing typos and improving discoverability.
 *
 * It supports configuration via KSP options for resource paths, file naming conventions,
 * and generated code details.
 *
 * @param environment The [SymbolProcessorEnvironment] provided by KSP.
 */
class MessageFilesProcessor(val environment: SymbolProcessorEnvironment) : SymbolProcessor{
    private val _debug = "debug"
    private val resourceDirPath = "resourceDirPath"
    private val resourcesDirName = "resourcesDirName"
    private val filePrefix = "filePrefix"
    private val fileExtension = "fileExtension"
    private val charSet = "charSet"
    private val genPackage = "genPackage"
    private val genFileName = "genFileName"
    private val genClassName = "genClassName"

    /** Default options for the processor, can be overridden via KSP arguments. */
    val myOptions = mutableMapOf<String,String>(
        _debug to "false",
        resourceDirPath to "",
        resourcesDirName to "resources",
        filePrefix to "Messages",
        fileExtension to ".properties",
        charSet to Charsets.UTF_8.toString(),
        genPackage to "com.ileveli.ksp",
        genFileName to "Messages",
        genClassName to "Messages",
    )
    private fun getOption(key: String):String = environment.options[key] ?: myOptions[key]!!
    val debug get() = getOption(_debug).equals("true",true)
    fun getCharSet(locale:String) = (environment.options[locale] ?: myOptions[locale]) ?: getOption(charSet)
    private val messageTagsByLocale = mutableMapOf<String, MutableSet<String>>()
    private var messageTags: Set<String> = emptySet()

    private var _callingRound = 0

    /**
     * The main processing step of the KSP processor.
     * It discovers message files, extracts keys, verifies consistency, and generates the Kotlin source code.
     * @param resolver Provides access to the KSP symbol graph.
     * @return A list of [KSAnnotated] symbols that were not processed (empty in this case).
     */
    override fun process(resolver: Resolver): List<KSAnnotated> {
        if(_callingRound > 0)
            return emptyList()

        environment.logger.warn("Entered MessageProcessor ${resolver.toString()}")

        resolveMessageFiles(resolveResourceFolder(resolver)).forEach {sourceFile->
            populateMessageTags(sourceFile)
        }

        messageTags = verifyMessageSets(messageTagsByLocale)
        if(messageTags.count() > 0)
            generateSourceCode(environment.codeGenerator)

        _callingRound += 1
        return emptyList()
    }
    private fun resolveResourceFolder(resolver:Resolver):File{
        val customResourcePath = File(getOption(resourceDirPath))
        if(customResourcePath.exists())
            return customResourcePath
        var path: Path? = null
        resolver.getAllFiles().forEach {kSFile ->
            path = Path(kSFile.filePath)
            while (path != null) {
                environment.logger.info("Processing path $path")
                val resourceDir = File(path.toFile(), getOption(resourcesDirName))
                if (resourceDir.exists())
                    return resourceDir
                path = path.parent
            }
        }
        environment.logger.warn("Unable to find 'resources' folder")
        return File("")
    }
    private fun resolveMessageFiles(resourceDir: File):List<File>{
        var filePaths = resourceDir.walk()
            .filter { it.isFile }
            .filter { it.name.startsWith(getOption(filePrefix)) }
            .filter { it.name.contains(getOption(fileExtension)) }
            .toList()

        if(filePaths.isEmpty())
            environment.logger.warn("Found 0 file of localized resources!!!")

        environment.logger.warn("Found ${filePaths.count()} paths")
        return filePaths
    }
    private fun populateMessageTags(file:File){
        var lineCount = 0
        val locale = file.nameWithoutExtension.split('_').let {
            if (it.size > 1) it.last() else ""
        }
        val charset_locale = "${charSet}_${locale}".trim('_')

        if(debug)
            environment.logger.info("Locale charset = $charset_locale for file: ${file.name}")

        file.bufferedReader(Charset.forName(getCharSet(charset_locale))).use { reader ->
            reader.lines().forEach{ text ->
                text.split("=").also {
                    if(it.first().startsWith("#") || it.count() < 2)
                        environment.logger.info("Skipping line($lineCount): ${text} of file: '${file.name}'")
                    else{
                        it.first().let { token->
                            if(!token.isEmpty()) {
                                val t = token.trim()
                                val filePath = file.path
                                messageTagsByLocale[filePath]?.add(t)
                                    ?: run {  messageTagsByLocale[filePath] = mutableSetOf(t) }
                            }
                        }
                    }

                }
                lineCount += 1
            }
        }
    }
    private fun verifyMessageSets(messageMap: MutableMap<String, MutableSet<String>>):Set<String>{
        val joinedSet:Set<String> = messageMap.flatMap {it.value }.toSet()
        val missingElements = mutableMapOf<String, Set<String>>()
        messageMap.forEach { (key, value) ->
            val missingSet = joinedSet - value
            if(missingSet.count() > 0)
                missingElements[key] = missingSet
        }
        if(!missingElements.isEmpty()){
            val errorMessage = missingElements.map { entry -> "File: '${entry.key}' is missing resource tags:[${entry.value.joinToString(",")}]" }
                .joinToString ("\n")
            environment.logger.error("Some tags missing from text resources locales\n${errorMessage}")
        }
        return joinedSet
    }
    private fun generateSourceCode(codeGenerator: CodeGenerator){

        val file = codeGenerator.createNewFile(
            Dependencies.ALL_FILES,
            getOption(genPackage),
            getOption(genFileName)
        )
        file.writer().use { writer ->
            writer.write(
"""
package ${getOption(genPackage)}

object ${getOption(genClassName)} {
    ${messageTags.joinToString("\n\t") { "const val $it:String = \"$it\"" }}
}
""".trimIndent())
        }
    }
}

/**
 * Provides an instance of [MessageFilesProcessor] to the KSP build system.
 */
class MessagesProcessorProvider : SymbolProcessorProvider {
    /**
     * Creates a new [MessageFilesProcessor] instance.
     * @param environment The [SymbolProcessorEnvironment] provided by KSP.
     * @return A new [MessageFilesProcessor].
     */
    override fun create(environment: SymbolProcessorEnvironment): SymbolProcessor {
        val debug = environment.options["debug"]?.equals("true",true) ?: false
        if(debug) {
            environment.logger.info("Entered HelloProcessor")
        }
        return MessageFilesProcessor(environment)
    }
}