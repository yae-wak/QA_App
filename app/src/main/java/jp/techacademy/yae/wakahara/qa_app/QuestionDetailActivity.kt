package jp.techacademy.yae.wakahara.qa_app

import android.content.Intent
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import kotlinx.android.synthetic.main.activity_question_detail.*

class QuestionDetailActivity : AppCompatActivity() {

    private lateinit var mQuestion: Question
    private lateinit var mAdapter: QuestionDetailListAdapter
    private var mFavoriteRef: DatabaseReference? = null

    private val mEventListener = object : ChildEventListener {
        override fun onChildAdded(dataSnapshot: DataSnapshot, p1: String?) {
            val map = dataSnapshot.value as Map<*, *>
            val answerUid = dataSnapshot.key ?: ""

            // 同じ AnswerUid のものが存在している場合は何もしない
            for (answer in mQuestion.answers) {
                if (answer.answerUid == answerUid) return
            }

            // 新しく投稿された回答を追加
            val body = map["body"] as? String ?: ""
            val name = map["name"] as? String ?: ""
            val uid = map["uid"] as? String ?: ""

            val answer = Answer(body, name, uid, answerUid)
            mQuestion.answers.add(answer)
            mAdapter.notifyDataSetChanged()
        }

        override fun onCancelled(p0: DatabaseError) {}

        override fun onChildMoved(p0: DataSnapshot, p1: String?) {}

        override fun onChildChanged(p0: DataSnapshot, p1: String?) {}

        override fun onChildRemoved(p0: DataSnapshot) {}
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_question_detail)

        // 渡ってきた質問を取得してタイトルを設定
        mQuestion = intent.extras!!.get("question") as Question
        title = mQuestion.title

        // ListView の準備
        mAdapter = QuestionDetailListAdapter(this, mQuestion)
        listView.adapter = mAdapter
        mAdapter.notifyDataSetChanged()

        // FAB をタップしたときの処理
        val user = FirebaseAuth.getInstance().currentUser
        fab.setOnClickListener {
            // ログインしていなければログイン画面に遷移
            if (user == null) {
                val intent = Intent(applicationContext, LoginActivity::class.java)
                startActivity(intent)
            }
            // ログインしていれば回答作成画面に遷移
            else {
                // Question を渡して回答作成画面を起動
                val intent = Intent(applicationContext, AnswerSendActivity::class.java)
                intent.putExtra("question", mQuestion)
                startActivity(intent)
            }
        }

        // 回答を作成して戻ってきたときのためにリスナーを登録
        val databaseReference = FirebaseDatabase.getInstance().reference
        val answerRef = databaseReference.child(ContentsPATH)
            .child(mQuestion.genre.toString()).child(mQuestion.questionUid).child(AnswersPATH)
        answerRef.addChildEventListener(mEventListener)

        // お気に入りスイッチがタップされたときの処理
        favoriteSwitch.setOnCheckedChangeListener { buttonView, isChecked ->
            // お気に入り情報を追加または削除
            if (isChecked) {
                val data = HashMap<String, String>()
                data["genre"] = mQuestion.genre.toString()
                mFavoriteRef!!.setValue(data)
            }
            else {
                mFavoriteRef!!.removeValue()
            }
        }
    }

    // ログイン画面から復帰することを想定して onResume にお気に入りスイッチの表示処理をかく
    override fun onResume() {
        super.onResume()

        // ログインしていれば、お気に入りスイッチを表示
        val user = FirebaseAuth.getInstance().currentUser
        if (user != null && favoriteSwitch.visibility == View.GONE) {
            favoriteSwitch.visibility = View.VISIBLE

            if (mFavoriteRef == null) {
                val databaseReference = FirebaseDatabase.getInstance().reference
                mFavoriteRef = databaseReference.child(UsersPATH).child(user.uid).child(FavoritesPath).child(mQuestion.questionUid)
            }

            // お気に入りスイッチの状態を設定
            mFavoriteRef!!.addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(dataSnapshot: DataSnapshot) {
                    favoriteSwitch.isChecked = dataSnapshot.exists()
                }
                override fun onCancelled(dataSnapshot: DatabaseError) {}
            })
        }
    }
}
