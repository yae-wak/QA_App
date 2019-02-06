package jp.techacademy.yae.wakahara.qa_app

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.ImageView
import android.widget.TextView

/**
 * String と Question クラスを受け取ることを想定したアダプタです。
 * String はジャンルを表示する行になります。
 */
class FavoriteAdapter(context: Context, objects: List<Any>) : ArrayAdapter<Any>(context, 0 , objects) {
    companion object {
        private val TYPE_GENRE = 0
        private val TYPE_QUESTION = 1;
    }

    private val mLayoutInflater: LayoutInflater
    init {
        mLayoutInflater = context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
    }

    /**
     * ジャンル行はタップしても何もしません。
     */
    override fun isEnabled(position: Int): Boolean {
        return getItem(position) is Question
    }

    override fun getViewTypeCount(): Int {
        return 2
    }

    override fun getItemViewType(position: Int): Int {
        return if (getItem(position) is String) TYPE_GENRE else TYPE_QUESTION
    }

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val view: View

        // ジャンルを表示
        if (getItemViewType(position) == TYPE_GENRE) {
            view = convertView ?: mLayoutInflater.inflate(R.layout.list_genre, parent, false)
            val genre = getItem(position) as String
            view.findViewById<TextView>(R.id.genreTextView).text = genre
        }
        // 質問を表示
        else {
            view = convertView ?: mLayoutInflater.inflate(R.layout.list_questions, parent, false)

            // View に表示するテキストを設定
            val question = getItem(position) as Question
            view.findViewById<TextView>(R.id.titleTextView).text = question.title
            view.findViewById<TextView>(R.id.nameTextView).text = question.name
            view.findViewById<TextView>(R.id.resTextView).text = question.answers.size.toString()

            // 画像を設定
            val bytes = question.imageBytes
            if (bytes.isNotEmpty()) {
                val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size).copy(Bitmap.Config.ARGB_8888, true)
                view.findViewById<ImageView>(R.id.imageView).setImageBitmap(bitmap)
            }
        }

        return view
    }
}