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

class QuestionDetailListAdapter(context: Context, private val mQuestion: Question) : BaseAdapter() {
    // どのレイアウトを使って表示させるかを判断するためのタイプを表す定数
    companion object {
        private val TYPE_QUESTION = 0
        private val TYPE_ANSWER = 1
    }

    private val mLayoutInflater: LayoutInflater
    init {
        mLayoutInflater = context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
    }

    override fun getItem(position: Int): Any {
        return mQuestion
    }

    override fun getItemId(position: Int): Long {
        return 0
    }

    override fun getCount(): Int {
        return mQuestion.answers.size + 1
    }

    override fun getItemViewType(position: Int): Int {
        return if (position == 0) TYPE_QUESTION else TYPE_ANSWER
    }

    override fun getViewTypeCount(): Int {
        return 2
    }

    override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
        val view: View

        // 質問を表示
        if (getItemViewType(position) == TYPE_QUESTION) {
            view = convertView ?: mLayoutInflater.inflate(R.layout.list_question_detail, parent, false)

            val bodyTextView = view.findViewById<TextView>(R.id.bodyTextView)
            val nameTextView = view.findViewById<TextView>(R.id.nameTextView)
            bodyTextView.text = mQuestion.body
            nameTextView.text = mQuestion.name

            val bytes = mQuestion.imageBytes
            if (bytes.isNotEmpty()) {
                val image = BitmapFactory.decodeByteArray(bytes, 0, bytes.size).copy(Bitmap.Config.ARGB_8888, true)
                val imageView = view.findViewById<ImageView>(R.id.imageView)
                imageView.setImageBitmap(image)
            }
        }
        // 回答を表示
        else {
            view = convertView ?: mLayoutInflater.inflate(R.layout.list_answers, parent, false)

            val answer = mQuestion.answers[position - 1]
            val bodyTextView = view.findViewById<TextView>(R.id.bodyTextView)
            val nameTextView = view.findViewById<TextView>(R.id.nameTextView)
            bodyTextView.text = answer.body
            nameTextView.text = answer.name
        }
        return view
    }
}