package jp.techacademy.yae.wakahara.qa_app

import android.content.Context
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.preference.PreferenceManager
import android.support.design.widget.Snackbar
import android.view.View
import android.view.inputmethod.InputMethodManager
import com.google.android.gms.tasks.OnCompleteListener
import com.google.firebase.auth.AuthResult
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import kotlinx.android.synthetic.main.activity_login.*

class LoginActivity : AppCompatActivity() {

    private lateinit var mAuth: FirebaseAuth
    private lateinit var mCreateAcountListener: OnCompleteListener<AuthResult>
    private lateinit var mLoginListener: OnCompleteListener<AuthResult>
    private lateinit var mDatabaseReference: DatabaseReference

    // アカウント作成時にフラグを立てて、ログイン処理後に Firebase に保存する
    private var mIsCreateAcount = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        // データベースへのリファレンスを取得
        mDatabaseReference = FirebaseDatabase.getInstance().reference

        // FirebaseAuth のオブジェクトを取得
        mAuth = FirebaseAuth.getInstance()

        // アカウント作成処理のリスナー
        mCreateAcountListener = OnCompleteListener { task ->
            // 成功した場合
            if (task.isSuccessful) {
                // ログイン
                login(emailText.text.toString(), passwordText.text.toString())
            }
            // 失敗した場合
            else {
                // エラーを表示
                val view = findViewById<View>(android.R.id.content)
                Snackbar.make(view, "アカウント作成に失敗しました", Snackbar.LENGTH_LONG).show()

                // プログレスバーを非表示
                progressBar.visibility = View.GONE
            }
        }

        // ログイン処理のリスナー
        mLoginListener = OnCompleteListener { task ->
            // 成功した場合
            val user = mAuth.currentUser
            val userRef = mDatabaseReference.child(UsersPATH).child(user!!.uid)

            if (task.isSuccessful) {
                // アカウント作成のとき
                if (mIsCreateAcount) {
                    // 表示名を Firebase に保存
                    val name = nameText.text.toString()
                    val data = HashMap<String, String>()
                    data["name"] = name
                    userRef.setValue(data)

                    // 表示名を Preference に保存
                    saveName(name)
                }
                // アカウント作成でないとき
                else {
                    // 表示名を Firebase から取得して Preference に保存
                    userRef.addListenerForSingleValueEvent(object: ValueEventListener {
                        override fun onDataChange(dataSnapshot: DataSnapshot) {
                            val data = dataSnapshot.value as Map<*, *>?
                            saveName(data!!["name"] as String)
                        }
                        override fun onCancelled(databaseError: DatabaseError) {}
                    })
                }

                // プログレスバーを非表示
                progressBar.visibility = View.GONE

                // アクティビティ終了
                finish()
            }
            else {
                // 失敗した場合、エラーを表示
                val view = findViewById<View>(android.R.id.content)
                Snackbar.make(view, "ログインに失敗しました", Snackbar.LENGTH_LONG).show()

                // プログレスバーを非表示
                progressBar.visibility = View.GONE
            }
        }

        // タイトルの設定
        this.title = "ログイン"

        // アカウント作成ボタンをタップしたときの処理
        createButton.setOnClickListener { view ->
            // キーボードが出ていたら閉じる
            val im = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            im.hideSoftInputFromWindow(view.windowToken,  InputMethodManager.HIDE_NOT_ALWAYS)

            // 入力された文字列を取得
            val email = emailText.text.toString()
            val password = passwordText.text.toString()
            val name = nameText.text.toString()

            // 正しく入力されていればアカウントを作成
            if (email.isNotBlank() && password.length >= 6 && name.isNotBlank()) {
                mIsCreateAcount = true      // ログイン時に表示名を Firebase に保存するようにフラグを立てる
                createAcount(email, password)
            }
            // 入力不正の場合はエラーを表示
            else {
                Snackbar.make(view, "正しく入力してください", Snackbar.LENGTH_LONG).show()
            }
        }

        // ログインボタンをタップしたときの処理
        loginButton.setOnClickListener { view ->
            // キーボードが出ていたら閉じる
            val im = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            im.hideSoftInputFromWindow(view.windowToken, InputMethodManager.HIDE_NOT_ALWAYS)

            // 入力された文字列を取得
            val email = emailText.text.toString()
            val password = passwordText.text.toString()

            // 正しく入力されていればログイン
            if (email.isNotBlank() && password.length >= 6) {
                mIsCreateAcount = false     // フラグを落としておく
                login(email, password)
            }
            // 入力不正の場合はエラーを表示
            else {
                Snackbar.make(view, "正しく入力してください", Snackbar.LENGTH_LONG).show()
            }
        }

    }

    private fun createAcount(email: String, password: String) {
        // プログレスバーを表示
        progressBar.visibility = View.VISIBLE

        // アカウントを作成
        mAuth.createUserWithEmailAndPassword(email, password).addOnCompleteListener(mCreateAcountListener)
    }

    private fun login(email: String, password: String) {
        // プログレスバーを表示
        progressBar.visibility = View.VISIBLE

        // ログイン
        mAuth.signInWithEmailAndPassword(email, password).addOnCompleteListener(mLoginListener)
    }

    private fun saveName(name: String) {
        // Preference に保存
        val sp = PreferenceManager.getDefaultSharedPreferences(this)
        val editor = sp.edit()
        editor.putString(NameKEY, name)
        editor.commit()
    }
}
