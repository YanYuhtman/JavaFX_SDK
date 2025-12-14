package com.ileveli

import com.google.devtools.ksp.processing.*
import com.google.devtools.ksp.symbol.*
import java.io.File
import java.nio.charset.Charset
import java.nio.file.Path
import kotlin.io.path.Path

class MessageFilesProcessor(val environment: SymbolProcessorEnvironment) : SymbolProcessor{
    private val _debug = "_debug"
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
        genPackage to "com.ileveli",
        genFileName to "Messages",
        genClassName to "Messages",
    )
    private fun getOption(key: String):String = environment.options[key] ?: myOptions[key]!!
    val debug get() = getOption(_debug).equals("true",true)
    val messageTags: MutableSet<String> = mutableSetOf()

    var _callingRound = 0
    override fun process(resolver: Resolver): List<KSAnnotated> {
        if(_callingRound > 0)
            return emptyList()

        environment.logger.warn("Entered MessageProcessor ${resolver.toString()}")
        resolver.getAllFiles().first().let { sourceFile ->
            resolveMessageFiles(sourceFile).forEach { sourceFile ->
                populateMessageTags(sourceFile)
            }
        }
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
        file.bufferedReader(Charset.forName(getOption(charSet))).use { reader ->
            reader.lines().forEach{ text ->
                text.split("=").also {
                    if(it.count() == 2){
                        it.first().let { token->
                            if(!token.isEmpty())
                                messageTags.add(token.trim())
                        }
                    }else if (debug)
                        environment.logger.warn("Skipping localization line($lineCount): ${text} of file: ${file.name}")
                }
                lineCount += 1
            }
        }
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
        if(debug)
            environment.logger.warn("Entered HelloProcessor")
        return MessageFilesProcessor(environment)
    }
}