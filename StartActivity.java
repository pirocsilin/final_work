package final_work.app;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.os.Bundle;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View.OnClickListener;
import android.view.View.OnTouchListener;
import android.view.View;
import android.view.ViewGroup.LayoutParams;
import android.view.animation.Animation;
import android.view.animation.TranslateAnimation;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.SeekBar;
import android.widget.TextView;
import java.util.ArrayList;
import java.util.Random;

public class StartActivity extends Activity{

    Button btnToGame, btnToResult;
    RelativeLayout MainArea;
    TextView textview;
    ImageView headerImage;
    DrawView myDraw;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        myDraw = new DrawView(this);
        setContentView(R.layout.activity_start);
        // Находим по id основной контейнер текущей activity из файла разметки
        MainArea = (RelativeLayout)findViewById(R.id.start);
        // Добавляем объект myDraw с помощью которого будем рисовать на экране
        MainArea.addView(myDraw,0);

        // Находим все элементы управления из activity_start.xml
        textview = (TextView)findViewById(R.id.rules);
        headerImage = (ImageView)findViewById(R.id.header);
        btnToGame = (Button)findViewById(R.id.start_game);
        btnToResult = (Button)findViewById(R.id.results);

        try {Thread.sleep(200);}
        catch(InterruptedException e){ }
    }
    // Метод для обработки нажатия кнопок
    public void onClick(View view){
        Button button = (Button)view;
        Intent intent;
        switch (button.getText().toString()){
            // Вызов MainActivity - игровой экран
            case "НАЧАТЬ ИГРУ":
                intent = new Intent(this, MainActivity.class);
                startActivity(intent);
                break;
            // Вызов ResultsActivity - зкран результатов
            case "РЕЗУЛЬТАТЫ":
                intent = new Intent(this, ResultsActivity.class);
                startActivity(intent);
                break;
        }
    }

    class Point{
        int x, y, cond;
        Point(int X, int Y){ x = X; y = Y;}
    }

    enum ConditionScreen{ INIT, DRAW_HEADER, STOP }

    // Класс для отрисовки объектов на экране
    class DrawView extends View implements Runnable{

        final int CELL = 15;        // размер ячейки для эффекта анимации
        final int dY = 36;          // отступ headerImage от верхней части экрана
        ConditionScreen condScreen; // состояния экрана
        Point M[][];                // массив точек
        Thread threads[];           // массив нитей
        int Rows,           // число ячеек по высоте экрана
            Columns,        // число ячеек по ширине экрана
            Width,          // ширина экрана
            Height;         // высота экрана
        Paint p;            // объект для настройки кисти

        public DrawView(Context context) {
            super(context);
            condScreen = ConditionScreen.INIT;
            p = new Paint();
            p.setStyle(Paint.Style.FILL);
        }

        public void Delay(int delay){
            try{ Thread.sleep(delay); }
            catch(InterruptedException e){}
        }

        // запуск анимации i-той строки массива М
        @Override
        public void run(){
            boolean CheckTread = false;
            // Пока все нити из массива threads выполняют работу
            // перерисовываем экран с задержкой 40 ms (анимация)
            do{
                for(Thread t : threads){
                    if(CheckTread = t.isAlive()){
                        invalidate();
                        Delay(40);
                        break;
                    }
                }
            }while(CheckTread);

            int New_Y_for_Header;
            // Перемещаем headerImage и массим ячеек вверх экрана
            do {
                for(int i = 0; i < Rows; i++)
                    for (int j = 0; j < Columns; j++)
                        M[i][j].y -= 3;

                New_Y_for_Header = (int)headerImage.getY() - 3;
                headerImage.setY(New_Y_for_Header);

                invalidate();
                Delay(3);
            }while(New_Y_for_Header > dY);

            condScreen = ConditionScreen.STOP;
            invalidate();
        }
        // Доступ к холсту canvas
        @Override
        protected void onDraw(Canvas canvas) {
            super.onDraw(canvas);
            canvas.drawColor(Color.BLACK);
            switch(condScreen){
                // Инициализация основных переменных
                case INIT:
                    condScreen = ConditionScreen.DRAW_HEADER;
                    Width = canvas.getWidth();
                    Height = canvas.getHeight();
                    // Количество ячеек которое уместится в ширину экрана
                    Columns = Width / CELL;
                    // Количество ячеек необходимое для перекрытия headerImage по высоте
                    Rows = Columns / 4 + 1;
                    M = new Point[Rows][Columns];
                    threads = new Thread[Rows];
                    headerImage.setY(Height/2-headerImage.getHeight()/2-dY);

                    for(int i=0; i<Rows; i++){
                        for(int j=0; j<Columns; j++)
                            M[i][j] = new Point(j * CELL, i * CELL+Height/2-headerImage.getHeight()/2);

                        Thread t = new Thread(new myThread("draw_"+i, i));
                        threads[i] = t;
                        t.start();
                    }
                    new Thread(this, "Control").start();
                    break;

                case STOP:
                    btnToGame.setVisibility(0);
                    btnToResult.setVisibility(0);
                    textview.setVisibility(0);

                case DRAW_HEADER:
                    for(int i=0; i < Rows; i++)
                        for(int j=0; j < Columns; j++)
                            if(M[i][j].cond == 1){
                               if((i+j) % 2 == 0)
                                    p.setColor(Color.WHITE);
                               else
                                    p.setARGB(255, 255, 204, 0);
                               canvas.drawRect(
                                        M[i][j].x,
                                        M[i][j].y,
                                        M[i][j].x + CELL,
                                        M[i][j].y + CELL, p);
                            }
                    break ;
            }
        }
    }

    class myThread extends Thread{
        Random random;
        int i;

        public myThread(String name, int I){
            super(name);
            i = I;
            random = new Random(I);
        }
        @Override
        public void run(){
            Get(0, myDraw.Columns);
        }

        public void Delay(){
            try{ Thread.sleep(40); }
            catch(InterruptedException e){}
        }
        // Интервал равный началу и концу строки делим на два используя случайный индекс r.
        // Меняем состояние ячейки (i-той строки r-го столбца) матрицы М на 1 (подлежит отрисовке).
        // Процедуру деления Левого и Правого интервалов (уменьшение интервалов)
        // продолжаем пока индексы начала и конца интервала не совпадут.
        public void Get(int L, int R){
            if(L != R){
                int r = random.nextInt(R-L)+L;
                myDraw.M[i][r].cond = 1;
                Delay();
                Get(L, r);
                Get(r+1, R);
            }
        }
    }
}
