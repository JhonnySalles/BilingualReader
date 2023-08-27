package br.com.ebook.fitz

class Outline(var title: String, var page: Int, var uri: String, var x: Float, var y: Float, var down: Array<Outline>?) {
    override fun toString(): String {
        val s = StringBuffer()
        s.append(page)
        s.append(": ")
        s.append(title)
        s.append(' ')
        s.append(uri)
        s.append('\n')
        if (down != null) {
            for (i in down!!.indices) {
                s.append('\t')
                s.append(down!![i])
                s.append('\n')
            }
        }
        s.deleteCharAt(s.length - 1)
        return s.toString()
    }
}