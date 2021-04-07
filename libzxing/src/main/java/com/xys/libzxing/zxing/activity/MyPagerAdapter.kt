import android.content.Context
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.viewpager.widget.PagerAdapter


public class MyPagerAdapter(context: Context, list: List<String>) : PagerAdapter() {
    private val mContext: Context
    private val mData: List<String>
    override fun getCount(): Int {
        return mData.size
    }

    override fun instantiateItem(container: ViewGroup, position: Int): Any {
        val title = TextView(mContext)
        title.text = "        "
        container.addView(title)
        return title
    }

    override fun getPageTitle(position: Int): CharSequence? {
        return mData[position]
    }
    override fun destroyItem(container: ViewGroup, position: Int, `object`: Any) {
        container.removeView(`object` as View)
    }

    override fun isViewFromObject(view: View, `object`: Any): Boolean {
        return view === `object`
    }

    init {
        mContext = context
        mData = list
    }
}