package bhformview;

import android.app.Activity;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.graphics.Rect;
import java.util.HashSet;
import java.util.List;
import java.util.ArrayList;

/**
 * Created by bighiung on 2017/10/15.
 */

public class BHFormView extends SurfaceView implements Runnable,SurfaceHolder.Callback {

    private SurfaceHolder mHolder; // 用于控制SurfaceView
    private volatile boolean flag = false; // 线程运行的标识，用于控制线程
    private Canvas mCanvas; // 声明一张画布
    private Paint paint;

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
    private boolean isDataLoading = false;

    /*设置数据委托对象*/
    public void setDelegate(FormViewDelegate del)
    {
        mDelegate = del;
    }
    /*设置数据源对象*/
    public void setDatasource(FormViewDatasource source)
    {
        mDatasource = source;
    }
    /*依据当前数据源刷新数据*/
    private BHFormView self;

    public BHFormView(Context context) {
        super(context);
        mHolder = getHolder(); // 获得SurfaceHolder对象
        mHolder.addCallback(this); // 为SurfaceView添加状态监听
        paint = new Paint(); // 创建一个画笔对象
        setFocusable(true); // 设置焦点
        self = this;
    }

    public BHFormView(Context context, AttributeSet attrs) {
        super(context, attrs);
        mHolder = getHolder(); // 获得SurfaceHolder对象
        mHolder.addCallback(this); // 为SurfaceView添加状态监听
        paint = new Paint(); // 创建一个画笔对象
        setFocusable(true); // 设置焦点
        self = this;
    }

    private void drawCellText(Canvas canvas, FMRect rect, String text, Typeface font) {
        paint.setFlags(Paint.ANTI_ALIAS_FLAG);
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(mDatasource.contentTextColor(this,rect.column,rect.row));

        float textSize = Math.max(rect.width / text.length(),mDatasource.contentTextSize(this,rect.column,rect.row));

        if (textSize >= rect.height * 0.8f)
        {
            textSize = rect.height * 0.8f - 2.0f;
        }

        if (textSize >= rect.width * 0.8f)
        {
            textSize = rect.width * 0.8f - 2.0f;
        }

        textSize *= 0.9;

//        paint.setElegantTextHeight(true);
        paint.setTypeface(font);

        paint.setTextSize(textSize);

        float textLength = paint.measureText(text);

        if (textLength >= rect.width * 0.85 )
        {

            textSize *= ( rect.width / textLength) * 0.85;

            paint.setTextSize(textSize);

        }

        Rect rectText = new Rect();
        paint.getTextBounds(text,0,text.length(),rectText);
        canvas.drawText(text, rect.x + rect.width / 2.0f - rectText.width() / 2.0f, rect.y + rect.height / 3.0f + rectText.height() / 2.0f, paint);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        isInit = true;

        if (mViewHeight != getMeasuredHeight() && isDataLoaded)
        {
            reloadData();
        }

        mViewHeight = getMeasuredHeight();
        mViewWidth = getMeasuredWidth();

        if (isDataLoaded){

            flag = true;

        }

    }

    private class ReloadDataThread extends Thread
    {

        public void run() {

            if (mDelegate != null)
            {

                ((Activity) getContext()).runOnUiThread(new Runnable() {
                    @Override
                    public void run() {

                        if (mDelegate != null)
                        {

                            mDelegate.didStartReload(self);

                        }
                    }
                });

            }

            loadDataFromDatasource();

            try {
                draw(); // 调用自定义画画方法
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                if (mCanvas != null) {
                    mHolder.unlockCanvasAndPost(mCanvas);//结束锁定画图，并提交改变。
                }
            }

            if (mDelegate != null)
            {

                ((Activity) getContext()).runOnUiThread(new Runnable() {
                    @Override
                    public void run() {

                        if (mDelegate != null)
                        {

                            mDelegate.didEndReload(self);

                        }
                    }
                });

            }

            isDataLoading = false;

        }

    }

    private class DrawThread extends Thread
    {

        public void run() {

            try {
                draw(); // 调用自定义画画方法
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                if (mCanvas != null) {
                    mHolder.unlockCanvasAndPost(mCanvas);//结束锁定画图，并提交改变。
                }
            }

            if (mDelegate != null)
            {

                ((Activity) getContext()).runOnUiThread(new Runnable() {
                    @Override
                    public void run() {

                        if (mDelegate != null)
                        {

                            mDelegate.didEndReload(self);

                        }
                    }
                });

            }

            isDataLoading = false;

        }

    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (event.getAction() == MotionEvent.ACTION_DOWN) {

            Log.i("clicked","clicked");

            if (mDelegate != null)
            {

                final float x = event.getX(),y = event.getY();

                Thread eventThread = new Thread(new Runnable() {
                    @Override
                    public void run() {

                        for (int ct = 0;ct != currentCellRects.size();ct ++)
                        {

                            FMRect rect = currentCellRects.get(ct);
                            if (rect.isPointInAreaOfSelf(x,y))
                            {
                                if (mDelegate != null)
                                {

                                    final FMRect rectangle = rect;

                                    ((Activity) getContext()).runOnUiThread(new Runnable() {
                                        @Override
                                        public void run() {

                                            if (mDelegate != null)
                                            {

                                                mDelegate.didSelected(self,rectangle.column,rectangle.row);

                                            }
                                        }
                                    });

                                }

                                break;
                            }
                        }

                    }
                });

                eventThread.start();

            }

        }
        return true;
    }

    public void reloadData()
    {

        if (!isDataLoading)
        {

            isDataLoading = true;

            ReloadDataThread reloadThread = new ReloadDataThread();

            reloadThread.start();

        }

    }

    //重新绘制，但不重新加载数据
    public void reDraw()
    {

        if (!isDataLoading)
        {

            isDataLoading = true;

            DrawThread reloadThread = new DrawThread();

            reloadThread.start();

        }

    }


    /*依据当前数据源刷新数据*/
    private void loadDataFromDatasource()
    {

        isDataLoading = true;

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

        ArrayList<FMRect> largerCells = new ArrayList<FMRect>();

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
                if (columnHeight > height)
                {
                    largerCells.add(rect);
                }

                for (int ct = 0;ct < largerCells.size();ct++)
                {
                    FMRect formerRect = largerCells.get(ct);
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

    }

    private class FMRect
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
            return isPointStrictInAreaOfSelf(rect.x,rect.y) || isPointStrictInAreaOfSelf(rect.x,rect.y + rect.height) || isPointStrictInAreaOfSelf(
                    rect.x + rect.width,rect.y) || isPointStrictInAreaOfSelf(rect.x + rect.width,rect.y + rect.height)||
                    isPointStrictInAreaOfSelf(rect.x + rect.width / 2.0f,rect.y)
                    ||isPointStrictInAreaOfSelf(rect.x ,rect.y + rect.height / 2.0f)
                    ||isPointStrictInAreaOfSelf(rect.x + rect.width,rect.y + rect.height / 2.0f)
                    ||isPointStrictInAreaOfSelf(rect.x + rect.width / 2.0f,rect.y + rect.height);
        }
        /*判断两个矩形是否碰撞,非严格，必须进入对方边界内区域才算*/
        final public boolean FMRectIntersectsRect(FMRect rect)
        {
            return isPointInAreaOfSelf(rect.x,rect.y) || isPointInAreaOfSelf(rect.x,rect.y + rect.height) || isPointInAreaOfSelf(
                    rect.x + rect.width,rect.y) || isPointInAreaOfSelf(rect.x + rect.width,rect.y + rect.height)||
                    isPointInAreaOfSelf(rect.x + rect.width / 2.0f,rect.y)||
                    isPointInAreaOfSelf(rect.x ,rect.y + rect.height / 2.0f)||
                    isPointInAreaOfSelf(rect.x + rect.width,rect.y + rect.height / 2.0f)||
                    isPointInAreaOfSelf(rect.x + rect.width / 2.0f,rect.y + rect.height);
        }

        final boolean isPointInAreaOfSelf(float _x,float _y)
        {
            return (_x > x) && (_y > y) && ((x + width) > _x) && ((y + height) > _y);
        }

        final boolean isPointStrictInAreaOfSelf(float _x,float _y)
        {
            return (_x >= x) && (_y >= y) && ((x + width) >= _x) && ((y + height) >= _y);
        }
    }

    public interface FormViewDelegate
    {
        /*某一行被点击*/
        void didSelected(BHFormView formView,int column,int row);
        /*数据reload开始*/
        void didStartReload(BHFormView formView);
        /*数据reload结束*/
        void didEndReload(BHFormView formView);
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

        /*某行某列的内容文字的颜色*/
        public float contentTextSize(BHFormView formView,int column,int row);

    }

    @Override
    public void surfaceCreated(SurfaceHolder surfaceHolder) {

//        drawThread = new Thread(this); // 创建一个线程对象
//        flag = true; // 把线程运行的标识设置成true
//        drawThread.start(); // 启动绘制线程

    }

    @Override
    public void surfaceChanged(SurfaceHolder surfaceHolder, int i, int i1, int i2) {

    }

    @Override
    public void surfaceDestroyed(SurfaceHolder surfaceHolder) {
        flag = false; // 把线程运行的标识设置成false
        mHolder.removeCallback(this);
    }

    @Override
    public void run() {


    }

    protected void draw()
    {
        mCanvas = mHolder.lockCanvas(); // 获得画布对象，开始对画布画画
        if (mCanvas != null &&  currentCellRects!= null)
        {
            //进行图形绘制
            //这里进行绘制
            for (int ct = 0;ct != currentCellRects.size();ct ++) {

                FMRect rect = currentCellRects.get(ct);
                paint.setStyle(Paint.Style.FILL);
                paint.setColor(mDatasource.contentColor(this,rect.column,rect.row));
                mCanvas.drawRect(rect.x,rect.y,rect.x+rect.width,rect.y+rect.height,paint);
                paint.setStrokeWidth(1);
                paint.setColor(mDatasource.BorderColor(this));
                paint.setStyle(Paint.Style.STROKE);
                mCanvas.drawRect(rect.x,rect.y,rect.x+rect.width,rect.y+rect.height,paint);
                //画文字
                String text = mDatasource.textForColumn(this,rect.column,rect.row);
                drawCellText(mCanvas,rect,text, mDatasource.fontOfContent(this));

            }

            //关闭重绘标志
            flag = false;

            isDataLoading = false;

        }

    }
}

