package br.com.fenix.bilingualreader.service.tokenizers

import android.annotation.TargetApi
import android.content.Context
import br.com.fenix.bilingualreader.util.helpers.FileUtil
import com.worksap.nlp.sudachi.Config
import com.worksap.nlp.sudachi.DictionaryFactory
import com.worksap.nlp.sudachi.PathAnchor

@TargetApi(26)
class SudachiTokenizer(
    context: Context,
    dictionary: com.worksap.nlp.sudachi.Dictionary? = null
) {

    private val SUDACHI_DATA_PATH = context.filesDir.absolutePath + "/sudachi"
    private val settings = """
        {
            "systemDict" : "${SUDACHI_DATA_PATH}/system_small.dic",
            "inputTextPlugin" : [
                { "class" : "com.worksap.nlp.sudachi.DefaultInputTextPlugin" },
                { "class" : "com.worksap.nlp.sudachi.ProlongedSoundMarkInputTextPlugin",
                  "prolongedSoundMarks": ["ー", "-", "⁓", "〜", "〰"],
                  "replacementSymbol": "ー"}
            ],
            "oovProviderPlugin" : [
                { "class" : "com.worksap.nlp.sudachi.MeCabOovProviderPlugin" },
                { "class" : "com.worksap.nlp.sudachi.SimpleOovProviderPlugin",
                  "oovPOS" : [ "補助記号", "一般", "*", "*", "*", "*" ],
                  "leftId" : 5968,
                  "rightId" : 5968,
                  "cost" : 3857 }
            ],
            "pathRewritePlugin" : [
                { "class" : "com.worksap.nlp.sudachi.JoinNumericPlugin",
                  "joinKanjiNumeric" : true },
                { "class" : "com.worksap.nlp.sudachi.JoinKatakanaOovPlugin",
                  "oovPOS" : [ "名詞", "普通名詞", "一般", "*", "*", "*" ],
                  "minLength" : 3
                }
            ]
        }
    """.trimIndent()

    var tokenizer: com.worksap.nlp.sudachi.Tokenizer? = null

    init {
        if (dictionary != null) {
            tokenizer = dictionary.create()
        } else {
            val mFileUtil = FileUtil(context)
            // Load language files from asset packs
            mFileUtil.copyAssetToFilesIfNotExist("sudachi/", "system_small.dic")
            //mFileUtil.copyAssetToFilesIfNotExist("sudachi/", "char.def")
            // Sudachi 0.7.0+: create(String) is deprecated; use Config.fromJsonString + create(Config)
            val config = Config.fromJsonString(
                settings,
                PathAnchor.classpath().andThen(PathAnchor.none())
            ).withFallback(Config.defaultConfig())
            val dict = DictionaryFactory().create(config)
            tokenizer = dict.create()
        }
    }

    fun tokenizeString(str: String): Iterable<List<com.worksap.nlp.sudachi.Morpheme>> {
        return tokenizer!!.tokenizeSentences(str)
    }
}
