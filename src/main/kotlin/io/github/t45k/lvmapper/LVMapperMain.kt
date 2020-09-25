package io.github.t45k.lvmapper

import io.github.t45k.lvmapper.entity.CodeBlock
import io.github.t45k.lvmapper.entity.TokenSequence
import io.github.t45k.lvmapper.tokenizer.LexicalAnalyzer
import io.github.t45k.lvmapper.tokenizer.SymbolSeparator
import io.github.t45k.lvmapper.tokenizer.Tokenizer
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.kotlin.toObservable
import java.io.File
import java.nio.file.Files
import java.nio.file.Paths

// 一旦リストに保持する
// スケーラビリティを考えると将来的にDBを使うかも
// IDはリストとかDBのインデックスで大丈夫そう
class LVMapperMain(private val config: LVMapperConfig) {

    private val tokenizer: Tokenizer =
        when (config.tokenizeMethod) {
            TokenizeMethod.LEXICAL_ANALYSIS -> LexicalAnalyzer()
            TokenizeMethod.SYMBOL_SEPARATION -> SymbolSeparator()
        }

    fun run() {
        val startTime = System.currentTimeMillis()
        val codeBlocks: List<CodeBlock> = collectSourceFiles(config.src)
            .flatMap(this::collectBlocks)
            .filter { it.tokenSequence.size in 10..2_000 }
            .toList()
            .blockingGet()

        println("${codeBlocks.size} code blocks have been extracted in ${convert((System.currentTimeMillis() - startTime) / 1000)} seconds.\n")

        val location = Location()
        val verification = Verification(codeBlocks)
        val progressMonitor = ProgressMonitor(codeBlocks.size)
        val clonePairs: List<Pair<Int, Int>> = codeBlocks
            .flatMapIndexed { index, codeBlock ->
                val seeds: List<Int> = createSeed(codeBlock.tokenSequence)
                val clonePairs: List<Pair<Int, Int>> = location.locate(seeds)
                    .filter { verification.verify(index, it) }
                    .map { index to it }

                location.put(seeds, index)
                progressMonitor.update(index + 1)

                clonePairs
            }

        println(clonePairs.size)

        val endTime = System.currentTimeMillis()
        println("time: ${convert((endTime - startTime) / 1000)}")

        val results = clonePairs.joinToString("\n") {
            val clone1 = codeBlocks[it.first]
            val split1 = clone1.fileName.split("/")
            val fileName1 = split1.last()
            val dir1 = split1[split1.size - 2]
            val clone2 = codeBlocks[it.second]
            val split2 = clone2.fileName.split("/")
            val fileName2 = split2.last()
            val dir2 = split2[split2.size - 2]
            "$dir1,$fileName1,${clone1.startLine},${clone1.endLine},$dir2,$fileName2,${clone2.startLine},${clone2.endLine}"
        }
        Files.writeString(Paths.get("result.csv"), results)
    }

    // TODO use rolling hash
    private fun createSeed(tokenSequence: TokenSequence): List<Int> =
        (0..(tokenSequence.size - config.windowSize))
            .map { tokenSequence.subList(it, it + config.windowSize).hashCode() }
            .distinct()

    @Suppress("BlockingMethodInNonBlockingContext")
    private fun collectSourceFiles(dir: File): Observable<File> =
        dir.walk()
            .filter { it.isFile && it.toString().endsWith(".java") }
            .toObservable()

    private fun collectBlocks(sourceFile: File): Observable<CodeBlock> =
        Observable.just(sourceFile)
            .flatMap { AST(tokenizer::tokenize).extractBlocks(it).toObservable() }
}

fun main(args: Array<String>) {
    val config: LVMapperConfig = parseArgs(args)
    LVMapperMain(config).run()
}
