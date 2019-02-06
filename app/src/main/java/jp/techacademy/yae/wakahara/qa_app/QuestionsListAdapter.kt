package jp.techacademy.yae.wakahara.qa_app

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.ImageView
import android.widget.TextView

class QuestionsListAdapter(context: Context) : BaseAdapter() {
    private var mLayoutInflater: LayoutInflater
    private var mQuestionList = ArrayList<Question>()

    init {
        mLayoutInflater = context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
    }

    override fun getItem(position: Int): Any {
        return mQuestionList[position]
    }

    override fun getItemId(position: Int): Long {
        return position.toLong()
    }

    override fun getCount(): Int {
        return mQuestionList.size
    }

    override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
        // 作成しておいたレイアウトを使用
        val view = convertView ?: mLayoutInflater.inflate(R.layout.list_questions, parent, false)

        // View に表示するテキストを設定
        val question = mQuestionList[position]
        view.findViewById<TextView>(R.id.titleTextView).text = question.title
        view.findViewById<TextView>(R.id.nameTextView).text = question.name
        view.findViewById<TextView>(R.id.resTextView).text = question.answers.size.toString()

        // 画像を設定
        val bytes = mQuestionList[position].imageBytes
        if (bytes.isNotEmpty()) {
            val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size).copy(Bitmap.Config.ARGB_8888, true)
            view.findViewById<ImageView>(R.id.imageView).setImageBitmap(bitmap)
        }

        return view
    }

    fun setQuestionList(questionList: ArrayList<Question>) {
        mQuestionList = questionList
    }
}