package jp.techacademy.yae.wakahara.qa_app

import android.Manifest
import android.app.Activity
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.graphics.drawable.BitmapDrawable
import android.net.Uri
import android.os.Build
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.preference.PreferenceManager
import android.provider.MediaStore
import android.support.design.widget.Snackbar
import android.util.Base64
import android.view.View
import android.view.inputmethod.InputMethodManager
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import kotlinx.android.synthetic.main.activity_question_send.*
import java.io.ByteArrayOutputStream
import java.lang.Exception

class QuestionSendActivity : AppCompatActivity() {

    companion object {
        private val PERMISSION_REQUEST_CODE = 100
        private val CHOOSER_REQUEST_CODE = 100
    }

    private var mGenre: Int = 0
    private var mPictureUri: Uri? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_question_send)

        // ジャンル番号を取得
        mGenre = intent.extras.getInt("genre")

        // UI の準備
        title = "質問作成"
        imageView.setOnClickListener { view -> onImageViewClick(view) }
        sendButton.setOnClickListener { view -> onSendButtonClick(view) }
    }

    /**
     * ImageView をタップしたときの処理
     * Android6.0以降のときは外部ストレージへの書き込みが許可されているか確認して showChooser() 呼び出し
     */
    private fun onImageViewClick(view: View) {
        // パーミッションの許可状態を確認
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
                // 許可されている
                showChooser()
            }
            else {
                // 許可されていないのでダイアログを表示
                requestPermissions(arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE), PERMISSION_REQUEST_CODE)
            }
        }
        else { showChooser() }
    }

    /**
     * リクエストした Permission の結果に対する処理
     */
    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        when(requestCode) {
            PERMISSION_REQUEST_CODE -> {
                // ユーザが許可したら実行
                if (grantResults[0] == PackageManager.PERMISSION_GRANTED) showChooser()
                return
            }
        }
    }

    /**
     * Intent連携でギャラリーとカメラを選択するダイアログを表示させる
     * ギャラリーから選択するIntentとカメラで撮影するIntentを作成して、さらにそれらを選択するIntentを作成してダイアログを表示させる
     */
    private fun showChooser() {
        // ギャラリーから選択するインテント
        val galleryIntent = Intent(Intent.ACTION_GET_CONTENT)
        galleryIntent.type = "image/*"
        galleryIntent.addCategory(Intent.CATEGORY_OPENABLE)

        // 画像を保存する URI を取得
        val filename = System.currentTimeMillis().toString() + ".jpg"
        val values = ContentValues()
        values.put(MediaStore.Images.Media.TITLE, filename)
        values.put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
        mPictureUri = contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)

        // カメラで撮影するインテント
        val cameraIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        cameraIntent.putExtra(MediaStore.EXTRA_OUTPUT, mPictureUri)

        // 選択ダイアログを表示するインテント
        val chooserIntent = Intent.createChooser(galleryIntent, "画像を取得")
        chooserIntent.putExtra(Intent.EXTRA_INITIAL_INTENTS, cameraIntent)

        // 結果を受け取る形式でインテントを発行
        startActivityForResult(chooserIntent, CHOOSER_REQUEST_CODE)
    }

    /**
     * Intent連携から戻ってきた時に画像を取得し、ImageViewに設定
     */
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == CHOOSER_REQUEST_CODE) {

            // OK でなければ データ削除
            if (resultCode != Activity.RESULT_OK) {
                if (mPictureUri != null) {
                    contentResolver.delete(mPictureUri!!, null, null)
                    mPictureUri = null
                }
                return
            }

            // 画像を取得（Gallery から取得した場合は data.data に入る）
            val uri = if (data == null || data.data == null) mPictureUri else data.data

            // URI から Bitmap を取得
            val image: Bitmap
            try {
                val inputStream = contentResolver.openInputStream(uri)
                image = BitmapFactory.decodeStream(inputStream)
                inputStream!!.close()
            }
            catch (e: Exception) { return }

            // 取得した Bimap の長辺を 500 ピクセルにリサイズ
            val scale = 500f / Math.max(image.width, image.height)
            val matrix = Matrix()
            matrix.postScale(scale, scale)
            val resizedImage = Bitmap.createBitmap(image, 0, 0, image.width, image.height, matrix, true)

            // リサイズした Bitmap を ImageView に設定
            imageView.setImageBitmap(resizedImage)

            mPictureUri = null
        }
    }

    /**
     * 投稿ボタンをタップしたときの処理
     */
    private fun onSendButtonClick(view: View) {
        // キーボードが出ていたら閉じる
        val im = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        im.hideSoftInputFromWindow(view.windowToken, InputMethodManager.HIDE_NOT_ALWAYS)

        // タイトルが入力されていない時はエラー表示
        if (titleText.text.isBlank()) {
            Snackbar.make(view, "タイトルを入力してください", Snackbar.LENGTH_LONG).show()
            return
        }

        // 質問が入力されていないときはエラー表示
        if (bodyText.text.isBlank()) {
            Snackbar.make(view, "質問を入力してください", Snackbar.LENGTH_LONG).show()
            return
        }

        // コンテンツのジャンル番号に相当するデータベース参照を取得
        val databaseReference = FirebaseDatabase.getInstance().reference
        val ganreRef = databaseReference.child(ContentsPATH).child(mGenre.toString())

        // Preferenece から名前取得
        val sp = PreferenceManager.getDefaultSharedPreferences(this@QuestionSendActivity)
        val name = sp.getString(NameKEY, "")

        // 保存するデータを作成
        val data = HashMap<String, String>()
        data["uid"] = FirebaseAuth.getInstance().currentUser!!.uid  // uid
        data["title"] = titleText.text.toString()
        data["body"] = bodyText.text.toString()
        data["name"] = name

        // 添付画像を取得
        val drawable = imageView.drawable as? BitmapDrawable

        // 添付画像が設定されていれば BASE64 エンコードして保存データに設定
        if (drawable != null) {
            val bitmap = drawable.bitmap
            val baos = ByteArrayOutputStream()
            bitmap.compress(Bitmap.CompressFormat.JPEG, 80, baos)
            val bitmapString = Base64.encodeToString(baos.toByteArray(), Base64.DEFAULT)
            data["image"] = bitmapString
        }

        // Firebase データベースに保存
        ganreRef.push().setValue(
            data,
            DatabaseReference.CompletionListener { error, reference -> onComplete(error, reference) }
            )

        // 画像保存に時間がかかる可能性があるので、プログレスバーを表示
        progressBar.visibility = View.VISIBLE
    }

    /**
     * Firebase への保存が終了したときの処理
     */
    private fun onComplete(databaseError: DatabaseError?, databaseReference: DatabaseReference) {
        // プログレスバーを非表示
        progressBar.visibility = View.GONE

        // エラーでなければ Activity を終了
        if (databaseError == null) {
            finish()
        }
        else {
            Snackbar.make(findViewById(R.id.content), "投稿に失敗しました", Snackbar.LENGTH_LONG).show()
        }
    }
}
