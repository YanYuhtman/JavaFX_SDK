package com.ileveli.ksp

import com.google.devtools.ksp.processing.*
import com.google.devtools.ksp.symbol.*
import java.io.File
import java.nio.charset.Charset
import java.nio.file.Path
import java.text.ParseException
import kotlin.io.path.Path

class MessageFilesProcessor(val environment: SymbolProcessorEnvironment) : SymbolProcessor{
    private val _debug = "debug"
    private val resourcesDirName = "resourcesDirName"
    private val filePrefix = "filePrefix"
    private val fileExtension = "fileExtension"
    private val charSet = "charSet"
    private val genPackage = "genPackage"
    private val genFileName = "genFileName"
    private val genClassName = "genClassName"

    val myOptions = mutableMapOf<String,String>(
        _debug to "false",
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
    override fun process(resolver: Resolver): List<KSAnnotated> {
        if(_callingRound > 0)
            return emptyList()

        environment.logger.warn("Entered MessageProcessor ${resolver.toString()}")
        resolver.getAllFiles().first().let { sourceFile ->
            resolveMessageFiles(sourceFile).forEach { sourceFile ->
                populateMessageTags(sourceFile)
            }
        }
        messageTags = verifyMessageSets(messageTagsByLocale)
        if(messageTags.count() > 0)
            generateSourceCode(environment.codeGenerator)

        _callingRound += 1
        return emptyList()
    }
    private fun resolveMessageFiles(sourceFile: KSFile):List<File>{
        var filePaths = emptyList<File>()
        var path: Path? = Path(sourceFile.filePath)
        do{
            path = path!!.parent
            val resourceDir = File(path!!.toFile(),getOption(resourcesDirName))
            if (resourceDir.exists()) {

                filePaths = resourceDir.walk()
                    .filter { it.isFile }
                    .filter { it.name.startsWith(getOption(filePrefix)) }
                    .filter { it.name.contains(getOption(fileExtension)) }
                    .toList()
                break
            }

        }while (path != null)
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
            genPackage,
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
class MessagesProcessorProvider : SymbolProcessorProvider {
    override fun create(environment: SymbolProcessorEnvironment): SymbolProcessor {
        val debug = environment.options["debug"]?.equals("true",true) ?: false
        if(debug) {
            environment.logger.info("Entered HelloProcessor")
        }
        return MessageFilesProcessor(environment)
    }
}