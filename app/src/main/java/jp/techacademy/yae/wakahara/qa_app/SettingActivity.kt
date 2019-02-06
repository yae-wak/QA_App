package jp.techacademy.yae.wakahara.qa_app

import android.content.Context
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.preference.PreferenceManager
import android.support.design.widget.Snackbar
import android.view.inputmethod.InputMethodManager
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import kotlinx.android.synthetic.main.activity_setting.*

class SettingActivity : AppCompatActivity() {

    private lateinit var mDatabaseReference: DatabaseReference

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_setting)

        // Preaference から表示名を取得して EditText に表示
        val sp = PreferenceManager.getDefaultSharedPreferences(this)
        val name = sp.getString(NameKEY, "")
        nameText.setText(name)

        // Firebase データベースへの参照を取得
        mDatabaseReference = FirebaseDatabase.getInstance().reference

        // タイトルを設定
        this.title = "設定"

        // 表示名変更ボタンをタップしたときの処理
        changeButton.setOnClickListener { view ->
            // キーボードが出ていたら閉じる
            val im = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            im.hideSoftInputFromWindow(view.windowToken, InputMethodManager.HIDE_NOT_ALWAYS)

            // ログイン済みのユーザを取得
            val user = FirebaseAuth.getInstance().currentUser

            // ログインしていない場合は何もしない
            if (user == null) {
                Snackbar.make(view, "ログインしていません", Snackbar.LENGTH_LONG).show()
                return@setOnClickListener
            }

            // 変更した表示名を Firebase に保存
            val userRef = mDatabaseReference.child(UsersPATH).child(user.uid)
            val data = HashMap<String, String>()
            data["name"] = name
            userRef.setValue(data)

            // 変更した表示名を Preference に保存
            val editor = sp.edit()
            editor.putString(NameKEY, name)
            editor.commit()

            Snackbar.make(view, "表示名を更新しました", Snackbar.LENGTH_LONG).show()
        }

        // ログアウトボタンをタップしたときの処理
        logoutButton.setOnClickListener { view ->
            FirebaseAuth.getInstance().signOut()
            nameText.setText("")
            Snackbar.make(view, "ログアウトしました", Snackbar.LENGTH_LONG).show()
        }
    }
}
