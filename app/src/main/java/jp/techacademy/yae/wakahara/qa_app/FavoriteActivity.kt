package jp.techacademy.yae.wakahara.qa_app

import android.content.Intent
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.util.Base64
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import kotlinx.android.synthetic.main.activity_favorite.*

class FavoriteActivity : AppCompatActivity(){

    private val mQuestionMap = HashMap<String, ArrayList<Question>>()
    private val mItemList = ArrayList<Any>()
    private lateinit var mAdapter: FavoriteAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_favorite)

        // タイトルを設定
        title = "お気に入り"

        // ユーザーを取得
        val user = FirebaseAuth.getInstance()

        // Firebase への参照を取得
        val databaseReference = FirebaseDatabase.getInstance().reference
        val favoriteRef = databaseReference.child(UsersPATH).child(user!!.uid!!).child(FavoritesPath)
        val questionRef = databaseReference.child(ContentsPATH)

        // お気に入り情報を取得
        favoriteRef.addListenerForSingleValueEvent(object: SimpleValueEventListener() {
            override fun onDataChange(dataSnapshot: DataSnapshot) {

                // お気に入りの一覧
                for(favoriteSnapshot in dataSnapshot.children) {
                    val favoriteData = favoriteSnapshot.value as Map<*, *>
                    val genre = favoriteData["genre"] as String
                    val questionUid = favoriteSnapshot.key!!

                    // 質問情報を取得
                    questionRef.child(genre).child(questionUid).addListenerForSingleValueEvent(object: SimpleValueEventListener() {
                        override fun onDataChange(dataSnapshot: DataSnapshot) {
                            onQuestionDataChange(dataSnapshot, genre)
                        }

                    })
                }
            }
        })

        // ListViewの設定
        mAdapter = FavoriteAdapter(this, mItemList)
        listView.adapter = mAdapter
        listView.setOnItemClickListener { parent, view, position, id ->
            // 質問をタップしたら詳細画面に遷移
            val intent = Intent(applicationContext, QuestionDetailActivity::class.java)
            intent.putExtra("question", mAdapter.getItem(position) as Question)
            startActivity(intent)
        }
    }

    /**
     * 質問情報を取得して表示
     */
    private fun onQuestionDataChange(dataSnapshot: DataSnapshot, genre: String) {
        val map = dataSnapshot.value as Map<*, *>

        // テキストデータを取得
        val title = map["title"] as? String ?: ""
        val body = map["body"] as? String ?: ""
        val name = map["name"] as? String ?: ""
        val uid = map["uid"] as? String ?: ""

        // 画像データを取得
        val imageString = map["image"] as? String ?: ""
        val bytes = if (imageString.isNotEmpty()) Base64.decode(imageString, Base64.DEFAULT) else byteArrayOf()

        // 回答リストを取得
        val answerList = ArrayList<Answer>()
        val answerMap = map["answers"] as Map<*, *>?
        if (answerMap != null) {
            for (key in answerMap.keys) {
                val temp = answerMap[key] as Map<*, *>
                val answerBody = temp["body"] as? String ?: ""
                val answerName = temp["name"] as? String ?: ""
                val answerUid = temp["uid"] as? String ?: ""
                val answer = Answer(answerBody, answerName, answerUid, key as String)
                answerList.add(answer)
            }
        }

        // 質問データを作成して保存
        val question = Question(title, body, name, uid, dataSnapshot.key ?: "", genre.toInt(), bytes, answerList)
        if (mQuestionMap.containsKey(genre) == false) mQuestionMap[genre] = ArrayList<Question>()
        mQuestionMap[genre]!!.add(question)

        // 表示用リストを作成
        mItemList.clear()
        for (key in mQuestionMap.keys) {
            val genreArray = resources.getStringArray(R.array.genre)
            mItemList.add(genreArray[key.toInt() - 1 ]) // ジャンル文字列を追加
            for (savedQuestion in mQuestionMap[key]!!) {
                mItemList.add(savedQuestion)
            }
        }
        mAdapter.notifyDataSetChanged()
    }
}

abstract class SimpleValueEventListener: ValueEventListener {
    abstract override fun onDataChange(dataSnapshot: DataSnapshot)
    override fun onCancelled(p0: DatabaseError) {}
}
