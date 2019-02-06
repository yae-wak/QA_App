package jp.techacademy.yae.wakahara.qa_app

import android.content.Context
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.preference.Preference
import android.preference.PreferenceManager
import android.support.design.widget.Snackbar
import android.view.View
import android.view.inputmethod.InputMethodManager
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import kotlinx.android.synthetic.main.activity_answer_send.*

class AnswerSendActivity : AppCompatActivity(), View.OnClickListener, DatabaseReference.CompletionListener {

    private lateinit var mQuestion: Question

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_answer_send)

        // 渡ってきた質問を取得
        mQuestion = intent.extras!!.get("question") as Question

        // UIの設定
        title = "回答作成"
        sendButton.setOnClickListener(this)
    }

    /**
     * 投稿ボタンがタップされたときの処理
     * Answer を Firebase に送信
     */
    override fun onClick(v: View) {
        // キーボードが出ていたら閉じる
        val im = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        im.hideSoftInputFromWindow(v.windowToken, InputMethodManager.HIDE_NOT_ALWAYS)

        // 回答が入力されていないときはエラーを表示するだけ
        if (answerEditText.text.isBlank()) {
            Snackbar.make(v, "回答を入力してください", Snackbar.LENGTH_LONG).show()
        }

        // データベースへの参照を取得
        val databalse = FirebaseDatabase.getInstance().reference
        val answerRef = databalse.child(ContentsPATH)
            .child(mQuestion.genre.toString()).child(mQuestion.questionUid).child(AnswersPATH)

        // 表示名を Preference から取得
        val sp = PreferenceManager.getDefaultSharedPreferences(this)
        val name = sp.getString(NameKEY, "")

        // 回答データを作成
        val data = HashMap<String, String>()
        data["uid"] = mQuestion.uid
        data["name"] = name!!
        data["body"] = answerEditText.text.toString()

        // 回答データを Firebase に保存
        progressBar.visibility = View.VISIBLE
        answerRef.push().setValue(data, this)
    }

    /**
     * Firebase への保存処理が終了したら呼ばれる
     */
    override fun onComplete(databaeError: DatabaseError?, databaseReference: DatabaseReference) {
        progressBar.visibility = View.GONE

        // 正常終了ならば Activity を終了、失敗したらエラーメッセージ
        if (databaeError == null) {
            finish()
        }
        else {
            Snackbar.make(findViewById(android.R.id.content), "投稿に失敗しました", Snackbar.LENGTH_LONG).show()
        }
    }
}
