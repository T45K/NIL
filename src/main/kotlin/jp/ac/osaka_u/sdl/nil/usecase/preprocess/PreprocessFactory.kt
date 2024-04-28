package jp.ac.osaka_u.sdl.nil.usecase.preprocess

import jp.ac.osaka_u.sdl.nil.NILConfig
import jp.ac.osaka_u.sdl.nil.usecase.preprocess.java.JavaPreprocess

class PreprocessFactory {
    companion object {
        fun create(config: NILConfig): Preprocess = JavaPreprocess(config)
    }
}
