package br.com.fenix.bilingualreader.view.adapter.detail.manga

import android.text.Html
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import br.com.fenix.bilingualreader.R
import br.com.fenix.bilingualreader.model.entity.Information
import br.com.fenix.bilingualreader.service.controller.ImageController
import br.com.fenix.bilingualreader.service.listener.InformationCardListener

class InformationRelatedViewHolder(itemView: View, private val listener: InformationCardListener) : RecyclerView.ViewHolder(itemView) {

    fun bind(information: Information) {
        val image = itemView.findViewById<ImageView>(R.id.manga_detail_information_related_related_cover)
        val title = itemView.findViewById<TextView>(R.id.manga_detail_information_related_related_title)
        val alternativeTitles = itemView.findViewById<TextView>(R.id.manga_detail_information_related_alternative_titles)
        val status = itemView.findViewById<TextView>(R.id.manga_detail_information_related_status)
        val publish = itemView.findViewById<TextView>(R.id.manga_detail_information_related_publish)
        val volumes = itemView.findViewById<TextView>(R.id.manga_detail_information_related_volumes_chapters)
        val authors = itemView.findViewById<TextView>(R.id.manga_detail_information_related_author)
        val genres = itemView.findViewById<TextView>(R.id.manga_detail_information_related_genres)
        val card = itemView.findViewById<LinearLayout>(R.id.manga_detail_information_related_related_card)

        card.setOnLongClickListener {
            listener.onClickLong(information.link)
            true
        }

        image.setImageBitmap(null)
        image.visibility = View.GONE

        if (information.imageLink != null)
            ImageController.instance.setImageAsync(itemView.context, information.imageLink!!, image)

        title.text = information.title
        alternativeTitles.text = Html.fromHtml(information.alternativeTitles)
        status.text = Html.fromHtml(information.status)
        publish.text = Html.fromHtml(information.release)
        volumes.text = Html.fromHtml(information.volumes + ", " + information.chapters)
        authors.text = Html.fromHtml(information.authors)
        genres.text = Html.fromHtml(information.genres)
    }

}