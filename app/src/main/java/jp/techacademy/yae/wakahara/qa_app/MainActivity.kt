package jp.techacademy.yae.wakahara.qa_app

import android.content.Intent
import android.os.Bundle
import android.support.design.widget.NavigationView
import android.support.design.widget.Snackbar
import android.support.v4.view.GravityCompat
import android.support.v7.app.ActionBarDrawerToggle
import android.support.v7.app.AppCompatActivity
import android.util.Base64
import android.view.Menu
import android.view.MenuItem
import android.view.View
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.app_bar_main.*
import kotlinx.android.synthetic.main.content_main.*

class MainActivity : AppCompatActivity(), NavigationView.OnNavigationItemSelectedListener {

    private var mGenre = 0

    private lateinit var mDatabaseReference: DatabaseReference
    private lateinit var mQuestionList: ArrayList<Question>
    private lateinit var mAdapter: QuestionsListAdapter

    private var mGenreRef: DatabaseReference? = null

    private val mEventListener = object : ChildEventListener {
        override fun onChildAdded(dataSnapshot: DataSnapshot, s: String?) {
            val map = dataSnapshot.value as Map<String, String>

            // テキストデータを取得
            val title = map["title"] ?: ""
            val body = map["body"] ?: ""
            val name = map["name"] ?: ""
            val uid = map["uid"] ?: ""

            // 画像データを取得
            val imageString = map["image"] ?: ""
            val bytes = if (imageString.isNotEmpty()) Base64.decode(imageString, Base64.DEFAULT) else byteArrayOf()

            // 回答リストを取得
            val answerList = ArrayList<Answer>()
            val answerMap = map["answers"] as Map<String, String>?
            if (answerMap != null) {
                for (key in answerMap.keys) {
                    val temp = answerMap[key] as Map<String, String>
                    val answerBody = temp["body"] ?: ""
                    val answerName = temp["name"] ?: ""
                    val answerUid = temp["uid"] ?: ""
                    val answer = Answer(answerBody, answerName, answerUid, key)
                    answerList.add(answer)
                }
            }

            // 質問データを作成して表示
            val question = Question(title, body, name, uid, dataSnapshot.key ?: "", mGenre, bytes, answerList)
            mQuestionList.add(question)
            mAdapter.notifyDataSetChanged()
        }

        override fun onChildChanged(dataSnapshot: DataSnapshot, s: String?) {
            val map = dataSnapshot.value as Map<String, String>

            // 変更のあった質問を探す
            for (question in mQuestionList) {
                if (dataSnapshot.key.equals(question.questionUid)) {
                    // このアプリでは変更があるのは回答のみ
                    question.answers.clear()
                    val answerMap = map["answers"] as Map<String, String>?
                    if (answerMap != null) {
                        for (key in answerMap.keys) {
                            val temp = answerMap[key] as Map<String, String>
                            val answerBody = temp["body"] ?: ""
                            val answerName = temp["name"] ?: ""
                            val answerUid = temp["uid"] ?: ""
                            val answer = Answer(answerBody, answerName, answerUid, key)
                            question.answers.add(answer)
                        }
                    }
                    // 更新を通知
                    mAdapter.notifyDataSetChanged()
                }
            }
        }

        override fun onCancelled(p0: DatabaseError) {}

        override fun onChildMoved(p0: DataSnapshot, p1: String?) {}

        override fun onChildRemoved(p0: DataSnapshot) {}
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        setSupportActionBar(toolbar)

        fab.setOnClickListener { view ->
            // ログイン済みのユーザを取得
            val user = FirebaseAuth.getInstance().currentUser

            // ログインされていなければログイン画面に遷移
            if (user == null) {
                val intent = Intent(applicationContext, LoginActivity::class.java)
                startActivity(intent)
            }
            // ジャンルを渡して質問作成画面を起動する
            else {
                // ジャンルを選択していない場合（mGenre == 0）はエラーを表示するだけ
                if (mGenre == 0) {
                    Snackbar.make(view, "ジャンルを選択してください", Snackbar.LENGTH_LONG).show()
                    return@setOnClickListener
                }
                val intent = Intent(applicationContext, QuestionSendActivity::class.java)
                intent.putExtra("genre", mGenre)
                startActivity(intent)
            }
        }

        // ナビゲーションドロワーの設定
        val toggle = ActionBarDrawerToggle(this, drawer, toolbar, R.string.app_name, R.string.app_name)
        drawer.addDrawerListener(toggle)
        toggle.syncState()

        // ナビゲーションのメニューをタップしたときの処理
        navView.setNavigationItemSelectedListener(this)

        // Firebase データベースへの参照を取得
        mDatabaseReference = FirebaseDatabase.getInstance().reference

        // ListView の準備
        mQuestionList = ArrayList<Question>()
        mAdapter = QuestionsListAdapter(this)
        mAdapter.setQuestionList(mQuestionList)
        listView.adapter = mAdapter
        mAdapter.notifyDataSetChanged()

        listView.setOnItemClickListener { parent, view, position, id ->
            // Question のインスタンスを渡して質問詳細画面を表示
            val intent = Intent(applicationContext, QuestionDetailActivity::class.java)
            intent.putExtra("question", mQuestionList[position])
            startActivity(intent)
        }

        // お気に入りボタンをタップしたときの処理
        favoriteButton.setOnClickListener {
            // お気に入り画面に遷移
            val intent = Intent(applicationContext, FavoriteActivity::class.java)
            startActivity(intent)
        }
    }

    override fun onResume() {
        super.onResume()

        // 1:趣味を既定の選択とする
        if (mGenre == 0) {
            onNavigationItemSelected(navView.menu.getItem(0))
        }

        // ログインしていたらお気に入りボタンを表示
        val user = FirebaseAuth.getInstance().currentUser
        favoriteButton.visibility = if (user == null) View.GONE else View.VISIBLE
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflate the menu; this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == R.id.action_settings) {
            val intent = Intent(this, SettingActivity::class.java)
            startActivity(intent)
            return true
        }

        return super.onOptionsItemSelected(item)
    }

    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        // タイトルを設定
        toolbar.title = item.title

        // ジャンルを保存
        when(item.itemId) {
            R.id.nav_hobby -> mGenre = 1
            R.id.nav_life -> mGenre = 2
            R.id.nav_health -> mGenre = 3
            R.id.nav_computer -> mGenre = 4
        }

        // 質問リストをクリアして ListView を更新
        mQuestionList.clear()
        mAdapter.notifyDataSetChanged()

        // 選択したジャンルにリスナーを登録
        if (mGenreRef != null) {
            mGenreRef!!.removeEventListener(mEventListener)
        }
        mGenreRef = mDatabaseReference.child(ContentsPATH).child(mGenre.toString())
        mGenreRef!!.addChildEventListener(mEventListener)

        // ドロワーを閉じる
        drawer.closeDrawer(GravityCompat.START)

        return true
    }
}
