package bhformview;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.graphics.Rect;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.concurrent.RecursiveAction;

/**
 * TODO: document your custom view class.
 */

public class BHFormView extends View {
    public BHFormView(Context context) {
        this(context, null);
    }
    public BHFormView(Context context, AttributeSet attrs) {
        super(context, attrs);

    }
    /*事件代理*/
    private FormViewDelegate mDelegate = null;
    /*数据源*/
    private FormViewDatasource mDatasource = null;
    private static HashSet<FMRect> reuseRects;
    /*当前处于*/
    private List<FMRect> currentCellRects = null;
    private int mViewWidth;
    private int mViewHeight;
    private boolean isInit = false;
    private boolean isDataLoaded = false;
    /*设置数据委托对象*/
    public void setDelegate(FormViewDelegate del)
    {
        mDelegate = del;
    }
    /*设置数据源对象*/
    public void setDatasource(FormViewDatasource source)
    {
        mDatasource = source;
        reloadData();
    }
    /*依据当前数据源刷新数据*/
    public void reloadData()
    {

        if (!isInit)
        {
            return;
        }

        if (mDatasource == null)
        {
            return;
        }

        int rowCount = mDatasource.numberOfRowsInFormView(this);
        List<Integer> columnCounts = new ArrayList<Integer>();
        currentCellRects = new ArrayList<FMRect>();

        for (int rowCounter = 0;rowCounter < rowCount;rowCounter++)
        {
            columnCounts.add(new Integer(mDatasource.numberOfColumns(this,rowCounter)));
        }
        float y = 0.f;

        for (int i = 0;i < rowCount;i++)
        {
            float height;
            height = mDatasource.heightForRow(this,i,mViewHeight,mViewWidth);
            float x = 0.f;
            //获取当前行的列数
            int columnCount = columnCounts.get(i).intValue();
            for (int j = 0;j < columnCount; j++)
            {
                float width;
                width = mDatasource.widthForColumn(this,j,i,mViewHeight,mViewWidth);
                float columnHeight;
                columnHeight = mDatasource.heightForRow(this,j,i,mViewHeight,mViewWidth);
                FMRect rect = new FMRect(x,y,width,columnHeight,j,i);
                for (int ct = 0;ct < currentCellRects.size();ct++)
                {
                    FMRect formerRect = currentCellRects.get(ct);
                    if (formerRect.FMRectIntersectsRect(rect))
                    {
                        //若x轴坐标有先前行的cell 延伸过来，则x轴坐标自动顺延
                        //有其他视图被延伸至本行
                        //并且正好占用了本行预期的开始位置
                        //则自动向右顺延
                        rect.x = formerRect.width + formerRect.x;
                    }
                }
                //单元格的位置
                currentCellRects.add(rect);
                x = rect.width + rect.x;
            }
            y += height;
        }
        isDataLoaded = true;
        invalidate();
    }


    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec)
    {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        isInit = true;
        mViewHeight = getMeasuredHeight();
        mViewWidth = getMeasuredWidth();
        reloadData();
        invalidate();
    }


    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        if ((isInit == true) && (isDataLoaded == true))
        {
            if (mDatasource == null)
            {
                return;
            }
            if (currentCellRects !=null)
            {
                Paint paint = new Paint();
                paint.setAntiAlias(true);
                for (int ct = 0;ct != currentCellRects.size();ct ++)
                {
                    //画边框
                    FMRect rect = currentCellRects.get(ct);
                    paint.setStyle(Paint.Style.FILL);
                    paint.setColor(mDatasource.contentColor(this,rect.column,rect.row));
                    canvas.drawRect(rect.x,rect.y,rect.x+rect.width,rect.y+rect.height,paint);
                    paint.setStrokeWidth(1);
                    paint.setColor(mDatasource.BorderColor(this));
                    paint.setStyle(Paint.Style.STROKE);
                    canvas.drawRect(rect.x,rect.y,rect.x+rect.width,rect.y+rect.height,paint);
                    //画文字
                    String text = mDatasource.textForColumn(this,rect.column,rect.row);
                    drawCellText(canvas,rect,text, mDatasource.fontOfContent(this));
                }
            }
        }
    }

    private void drawCellText(Canvas canvas,FMRect rect,String text,Typeface font) {
        Paint paint = new Paint();
        paint.setFlags(Paint.ANTI_ALIAS_FLAG);
        paint.setColor(mDatasource.contentTextColor(this,rect.column,rect.row));
        float textSize = rect.width / text.length();
        if (textSize >= rect.height * 0.8f)
        {
            textSize = rect.height * 0.8f - 2.0f;
        }
        textSize *= 0.9;
        paint.setTextSize(textSize);

        if (font == null)
        {
            font = Typeface.DEFAULT;
        }

        paint.setTypeface(font);
        Rect rectText = new Rect();
        paint.getTextBounds(text,0,text.length(),rectText);
        canvas.drawText(text, rect.x + rect.width / 2.0f - rectText.width() / 2.0f, rect.y + rect.height / 2.0f + + rectText.height() / 2.0f, paint);
    }


    public class FMRect
    {
        public float x;
        public float y;
        public float width;
        public float height;
        public int column;
        public int row;
        FMRect (float _x,float _y,float _width,float _height,int _column,int _row)
        {
            x = _x;
            y = _y;
            width = _width;
            height = _height;
            column = _column;
            row = _row;
        }
        /*判断两个矩形是否碰撞,严格，有边界有相交就算*/
        final public boolean FMRectStrictIntersectsRect(FMRect rect)
        {
            return hasStrictInterSection(x,x+width,rect.x
                    ,rect.width)&&hasStrictInterSection(y,y+height,rect.y,rect.y+rect.height);
        }
        /*判断两个矩形是否碰撞,非严格，必须进入对方边界内区域才算*/
        final public boolean FMRectIntersectsRect(FMRect rect)
        {
            return hasInterSection(x,x+width,rect.x
                    ,rect.width)&&hasInterSection(y,y+height,rect.y,rect.y+rect.height);
        }

        final boolean isPointInAreaOfSelf(float _x,float _y)
        {
            return (_x > x) && (_y > y) && ((x + width) > _x) && ((y + height) > _y);
        }

        final boolean isPointStrictInAreaOfSelf(float _x,float _y)
        {
            return (_x >= x) && (_y >= y) && ((x + width) >= _x) && ((y + height) >= _y);
        }

        //判断两个区间是否存在交集（非严格，边界交叉也算无交集）
        final boolean hasInterSection(float start1,float end1,float start2,float end2)
        {
            if (start1 > end1)
            {
                start1 += end1;end1 -= start1;start1 += end1;end1 = -end1;
            }
            if (start2 > end2)
            {
                start2 += end2;end2 -= start2;start2 += end2;end2 = -end1;
            }
            return !((end1 <= start2) || (end2 <= start1));
        }

        //判断两个区间是否存在交集（严格，边界交叉也算有交集）
        final boolean hasStrictInterSection(float start1,float end1,float start2,float end2)
        {
            if (start1 > end1)
            {
                start1 += end1;end1 -= start1;start1 += end1;end1 = -end1;
            }
            if (start2 > end2)
            {
                start2 += end2;end2 -= start2;start2 += end2;end2 = -end1;
            }
            return !((end1 < start2) || (end2 < start1));
        }
    }


    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (event.getAction() == MotionEvent.ACTION_DOWN) {
            for (int ct = 0;ct != currentCellRects.size();ct ++)
            {
                FMRect rect = currentCellRects.get(ct);
                if (rect.isPointInAreaOfSelf(event.getX(),event.getY()))
                {
                    if (mDelegate != null)
                    {
                        mDelegate.didSelected(this,rect.column,rect.row);
                    }
                }
            }
        }
        return super.onTouchEvent(event);
    }

    public interface FormViewDelegate
    {
        /*某一行被点击*/
        void didSelected(BHFormView formView,int column,int row);
    }

    public interface FormViewDatasource
    {
        /*获取函数*/
        public int numberOfRowsInFormView(BHFormView formView);
        /*获取某一行的列数*/
        public int numberOfColumns(BHFormView formView,int row);
        /*返回某一行的基准的高度*/
        public float heightForRow(BHFormView formView,int row,int viewHeight,int viewWidth);
        /*返回某一单元格的高度*/
        public float heightForRow(BHFormView formView,int column,int row,int viewHeight,int viewWidth);
        /*返回某一个单元格的宽度*/
        public float widthForColumn(BHFormView formView,int column,int row,int viewHeight,int viewWidth);
        /*返回内容字体*/
        public Typeface fontOfContent(BHFormView formView);
        /*某一个单元格的内容*/
        public String textForColumn(BHFormView formView,int Column,int row);
        /*获得边框颜色*/
        public int BorderColor(BHFormView formView);
        /*某行某列的内容颜色*/
        public int contentColor(BHFormView formView,int column,int row);
        /*某行某列的内容文字的颜色*/
        public int contentTextColor(BHFormView formView,int column,int row);
    }
}

