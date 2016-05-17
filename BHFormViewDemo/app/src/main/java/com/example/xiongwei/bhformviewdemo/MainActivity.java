package com.example.xiongwei.bhformviewdemo;

import android.graphics.Color;
import android.graphics.Typeface;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;

import bhformview.BHFormView;

public class MainActivity extends AppCompatActivity {


    private BHFormView mFromView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mFromView = (BHFormView)findViewById(R.id.formView);
        mFromView.setDatasource(new BHFormView.FormViewDatasource() {
            @Override
            public int numberOfRowsInFormView(BHFormView formView) {
                return 5;
            }
            @Override
            public int numberOfColumns(BHFormView formView, int row) {
                switch (row) {
                    case 0:
                        return 4;
                    case 1:
                        return 3;
                    case 2:
                        return 4;
                    case 3:
                    case 4:
                        return 3;
                    default:
                        break;
                }
                return 0;
            }

            @Override
            public float heightForRow(BHFormView formView, int row, int viewHeight, int viewWidth) {
                return viewHeight / 5;
            }

            @Override
            public float heightForRow(BHFormView formView, int column, int row, int viewHeight, int viewWidth) {
                if (column == 0) {
                    switch (row) {
                        case 0:
                            return (viewHeight / 5) * 2;
                        case 2:
                        {
                            return (viewHeight / 5) * 3;

                        }
                        default:
                            return viewHeight / 5;
                    }
                }
                return viewHeight / 5;
            }

            @Override
            public float widthForColumn(BHFormView formView, int column, int row, int viewHeight, int viewWidth) {
                int width = 0;
                if (column == 0 && (row == 0 || row == 2)) {
                    width = viewWidth / 7;
                }
                else
                {
                    width = (viewWidth / 7) * 2;
                }
                return width;
            }

            @Override
            public Typeface fontOfContent(BHFormView formView) {
                return null;
            }

            @Override
            public String textForColumn(BHFormView formView, int Column, int row) {
                return row + "行" + Column + "列";
            }

            @Override
            public int BorderColor(BHFormView formView) {
                return Color.argb(255,0,0,0);
            }

            @Override
            public int contentColor(BHFormView formView, int column, int row) {
                int R = column * 25;
                int G = row * 20;
                int B = (column * row * 10) % 255;
                int A = (20 * (row+1));
                return Color.argb(A,R,G,B);
            }

            @Override
            public int contentTextColor(BHFormView formView, int column, int row) {
                return Color.argb(255,0,0,0);
            }
        });
    }
}
