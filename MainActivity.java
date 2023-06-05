package final_work.app;

import androidx.appcompat.app.AppCompatActivity;

import android.content.ContentValues;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import java.sql.Date;
import java.text.SimpleDateFormat;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.graphics.Typeface;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.util.Log;
import android.app.Activity;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Path;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup.LayoutParams;
import android.view.MotionEvent;
import android.widget.Button;
import android.widget.RelativeLayout;
import android.view.View.OnTouchListener;
import android.view.View.OnClickListener;

import java.text.SimpleDateFormat;
import java.util.Random;

public class MainActivity extends Activity implements OnTouchListener{

    int Intention;          // Намерение игрока - удалить или создать ячейку
    SeekBar seekBar;        // Ползунок для выбора размера ячейки
    DrawView myDraw;        // Объект для доступа к холсту
    RelativeLayout GameArea;// Контейнер для View компонент activity
                            // Кнопки управления игровым процессом:
    static Button btnToGame, btnResult, btnRules, btnRestart;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Находим файл разиметки экрана по ID
        GameArea = (RelativeLayout)findViewById(R.id.game);
        myDraw = new DrawView(this);        // Создаем объект для доступа к Canvas,
        myDraw.setOnTouchListener(this);    // крепим к нему обработчик касаний,
        GameArea.addView(myDraw,0);         // и добавляем объект в контейнер GameArea.

        // Установка состояния игрового процесса при запуске activity
        DrawView.cond = ConditionType.START_INIT;
        // Инициализация объекта MediaPlayer (установка мелодии applod.mp3)
        myDraw.mPlayer = MediaPlayer.create(this, R.raw.applod);

        // Находим все нужные изображения по ID ресурса
        myDraw.origWall = BitmapFactory.decodeResource(getResources(), R.drawable.wall);
        myDraw.origArrow = BitmapFactory.decodeResource(getResources(), R.drawable.arrow_3);
        myDraw.origRevers = BitmapFactory.decodeResource(getResources(), R.drawable.revers);
        myDraw.HERRO = (ImageView)findViewById(R.id.hero);
        myDraw.Balls = new int[]{R.drawable.red_ball, R.drawable.blue_ball,
                        R.drawable.yello_ball, R.drawable.green_ball, R.drawable.gold_ball};

        // Находим все элемениы управления
        btnToGame = (Button)findViewById(R.id.btn_seek);
        btnRestart = (Button)findViewById(R.id.restart);
        btnRules = (Button)findViewById(R.id.rules);
        btnResult = (Button)findViewById(R.id.results);
        seekBar = (SeekBar)findViewById(R.id.seek);

        // Устанавливаем seekBar как слушателя изменения значения в SeekBar
        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            // SeekBar.OnSeekBarChangeListener позволяет установить три метода-обработчика:
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                // Изменяем размер ячейки при перемещении ползунка
                myDraw.CELL = progress;
                myDraw.invalidate();
            }
            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {}
            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {}
        });
    }
    // Метод для обработки нажатия кнопок
    public void onClick(View view){
        Button button = (Button)view;
        switch (button.getText().toString()){
            // Продолжаем игру после выбора размера ячейки
            case "Продолжить":
                DrawView.cond = ConditionType.START_POSITION;
                seekBar.setVisibility(8);
                btnToGame.setVisibility(8);
                myDraw.invalidate();
                break;
            // Перезапуск игрового процесса
            case "ПОВТОРИТЬ ИГРУ":
                myDraw.BestResult = myDraw.dbHelper.MaxResult(myDraw.db);
                DrawView.cond = ConditionType.START_INIT;
                btnRestart.setVisibility(8);
                btnRules.setVisibility(8);
                btnResult.setVisibility(8);
                myDraw.HERRO.setVisibility(8);
                seekBar.setVisibility(0);
                btnToGame.setVisibility(0);
                myDraw.PathExists = false;
                myDraw.FinishGame = false;
                myDraw.StopCongret = false;
                myDraw.StartPos = -1;
                myDraw.EndPos = -1;
                myDraw.Score = 0;
                myDraw.B_pts = null;
                myDraw.scale = 1;
                myDraw.invalidate();
                break;
            // Запускаем activity StartActivity (первый экран)
            case "НА ГЛАВНУЮ":
                this.startActivity(new Intent(this, StartActivity.class));
                break;
            // Запускаем activity ResultsActivity (экран результатов)
            case "РЕЗУЛЬТАТЫ":
                this.startActivity(new Intent(this, ResultsActivity.class));
                break;
        }
    }
    // Обработка касания и перемещения пальца по экрану
    @Override
    public boolean onTouch(View v, MotionEvent e) {
        if(DrawView.cond == ConditionType.STOP || DrawView.cond == ConditionType.CHECK_PATH ||
           DrawView.cond == ConditionType.FINDE_PATH || DrawView.cond == ConditionType.GAME_SCORE)
           return false;

        // Вычисляем индексы элемента массива М
        int j = ((int)e.getX()-myDraw.sp)/myDraw.CELL;
        int i = ((int)e.getY()-myDraw.sp)/myDraw.CELL;
        // Объект который вызвал метод
        DrawView myDraw = (DrawView)v;
        // Событие, ктоторое поизошло
        int Action = e.getAction();

        // Инициалицация стартовой и финишной позиций
        if(Action == MotionEvent.ACTION_DOWN){
            if(DrawView.cond == ConditionType.START_POSITION && j == 0 && i >= 1 && i < myDraw.Area/myDraw.CELL-1)
                myDraw.StartPos = i;
            if(DrawView.cond == ConditionType.END_POSITION && j == myDraw.Area/myDraw.CELL-1 && i >= 1 && i < myDraw.Area/myDraw.CELL-1)
                myDraw.EndPos = i;
        }

        // Если попали на игровую поверхность и StartPos, EndPos опредены
        if(i >= 1 && j >=1 && i < myDraw.Area/myDraw.CELL-1 && j < myDraw.Area/myDraw.CELL-1 &&
                                  myDraw.StartPos != -1 && myDraw.EndPos != -1){
            switch(Action){
                // Сохраняем намерение пользователя (1 - отрисовка ячейки, 0 - удаление)
                case MotionEvent.ACTION_DOWN:
                    Intention = myDraw.M[i][j].cond==0 ? 1 : 0;
                // При перемещении пальца, меняем состояние новой ячейки на противоположное
                case MotionEvent.ACTION_MOVE:
                    if(myDraw.M[i][j].cond != Intention && CheckCell(myDraw.M[i][j],i,j)){
                        myDraw.M[i][j].cond = Intention;
                        myDraw.invalidate();
                    }
                    break;
            }
        }
        return true;
    }
    // Проверка ячейки на правила построения лабиринта
    public boolean CheckCell(Pt pt, int i, int j){
        return  myDraw.M[i][j-1].cond==1   &&
                myDraw.M[i+1][j-1].cond==1 &&
                myDraw.M[i+1][j].cond==1   ||
                myDraw.M[i+1][j].cond==1   &&
                myDraw.M[i+1][j+1].cond==1 &&
                myDraw.M[i][j+1].cond==1   ||
                myDraw.M[i][j+1].cond==1   &&
                myDraw.M[i-1][j+1].cond==1 &&
                myDraw.M[i-1][j].cond==1   ||
                myDraw.M[i-1][j].cond==1   &&
                myDraw.M[i-1][j-1].cond==1 &&
                myDraw.M[i][j-1].cond==1   ?
                false : true;
    }
}
// Состояния игрового процесса
enum ConditionType{
    START_INIT,     // Инициализация основных переменных
    START_POSITION, // Выбор стартовой позиции
    END_POSITION,   // Выбор Финишной позиции
    GAME_TIME,      // Создание лабиринтра пользователем
    CHECK_PATH,     // Проверка существования пути от старта до финиша
    FINDE_PATH,     // Прохождение лабиринта персонажем
    GAME_SCORE,     // Подсчет очков пользователя
    STOP            // Остановка игрового процесса
}
// Элемент массива M[][] (массив хранит всю информацию о лабиринте)
class Pt{
    int x, y,   // точка левого верхнего угла экрана для отрисовки ячейки;
        cond,   // состояние ячейки (1 - путь есть, 0 - пути нет);
        path,   // состояние пути (1 - не пройден, 2 - пройден единожды, 3 - дважды);
        rotate; // rotate - угол поворота стрелок на пути от входа к выходу
    Pt(int X, int Y){ x = X; y = Y;}
}
// Данный класс используем для получения даступа к Canvas через метод onDraw(Canvas canvas)
class DrawView extends View implements Runnable{
    // Объекты для хранения изображений
    Bitmap origWall, Wall,          // Стенка для лабиринта
           origArrow, Arrow,        // Стрелка направления пути
           origRevers, Revers;      // Крестик для отметки пройденной дважды ячейки
    ImageView HERRO;                // Персонаж лабиринта
    Matrix matrix;                  // Объект для трансформации изображений
    final int TimeForGame = 60;     // Время игрового процесса
    static ConditionType cond;      // Состояние игрового процесса
    boolean PathExists = false,     // Результат проверки наличия пути от входа до выхода
            FinishGame = false,     // Результат проверки окончания игрового процесса
            StopCongret = false;    // Завершение поздравления игрока
    int Score = 0,                  // Счет игры
        Alpha[] = {40, 40, 40, 40, 40}, // Установка прозрачности для колец таймера
        Timer = TimeForGame,        // Таймер игрового процесса
        StartPos = -1,              // Стартовая позиция персонажа
        EndPos = -1,                // Финишная позиция персонажа
        CELL = 75,                  // Размер ячейки в PX, min 50, max 100
        Area, N,                    // Размер игрового поля в PX; N - размерность массива М
        sp, Width, Height,          // Стартовая позиция игрового поля; Ширина и Высота экрана;
        R=50, G=100, B=200,         // Компоненты RGB для выделения области старта в финиша
        BestResult,                 // Лучший результат прошлых игр
        numBall=40,                 // Количество воздушных шариков
        Balls[];                    // Виды воздушных шариков
    Balloon B_pts[];                  // Координаты воздушных шариков
    Pt M[][];                       // Координаты и состояние ячеек коридроров
    Paint p;                        // Кисть для рисования на Canvas
    Path path;                      // Хранение сложных объектов
    float scale = 1;                // Размер графического объекта
    MediaPlayer mPlayer;            // Объект для проигрывания поздравительной мелодии
    DB_Helper dbHelper;             // Объект для помощи манипуляций с БД
    SQLiteDatabase db;              // Объект для подключения к БД
    class Balloon {
        float x; float y; Bitmap btp;
        Balloon(float X, float Y, Bitmap Btp){x=X; y=Y; btp=Btp;}}

    public DrawView(Context context) {
        super(context);
        p = new Paint();
        path = new Path();
        matrix = new Matrix();
        dbHelper = new DB_Helper(getContext());
        db = dbHelper.getWritableDatabase();
        BestResult = dbHelper.MaxResult(db);
    }
    // Таймер игрового процесса
    public void run(){
        String Name = Thread.currentThread().getName();
        try{
            switch(Name){
                // Уменьшаем время игрового процесса
                // и зменяем прозрачность колец таймера
                case "GameTimer":
                    while(--Timer >= 0){
                        for(int i=0; i<Alpha.length; i++){
                            Alpha[i] = 250 - i * 40;
                            this.invalidate();
                            Thread.sleep(200);
                            Alpha[i] = 40;
                        }
                    }
                    Timer = TimeForGame;
                    cond = ConditionType.CHECK_PATH;
                    this.invalidate();
                    break;

                // Пересчет координат воздушных шаров
                case "Congrates":
                    int maxY = 0, takt = 0;
                    float delta = 0.005f;
                    mPlayer.start();
                    Thread.sleep(100);
                    while(B_pts[maxY].y >= -200){
                        for(int i=0; i<numBall; i++){
                            maxY = B_pts[i].y > B_pts[maxY].y ? i : maxY;
                            B_pts[i].y-= i%4 == 0 ? 3 : i%3 == 0 ? 3.5 : i%2 == 0 ? 4 : 4.5;
                        }
                        delta = scale <= 1 ? 0.005f : scale >= 1.13 ? -0.005f : delta;
                        scale += delta;
                        this.invalidate();
                        Thread.sleep(5);
                    }
                    StopCongret = true;
                    this.invalidate();
                    break;
            }
        }
        catch(InterruptedException e){ }
    }
    // Включение, выключение видимости кнопок
    public void ButtonControl(int condition){
        MainActivity.btnRestart.setVisibility(condition);
        MainActivity.btnRules.setVisibility(condition);
        MainActivity.btnResult.setVisibility(condition);
    }
    // Метод вывзывается каждый раз когда нужно перерисовать холст
    @Override
    protected void onDraw(Canvas canvas) {
        // Заливка холста фоновым цветом
        canvas.drawARGB(50, 102, 204, 255);

        // Инициализация основных переменных
        if(cond == ConditionType.START_INIT){
            Width = canvas.getWidth();
            Height = canvas.getHeight();
            Area = Width - Width % CELL;
            sp = (Width - Area) / 2;
            N = Area / CELL;
            M = new Pt[N][N];

            // Записываем координаты левого верхнего угла ij-ой ячейки
            for(int i=0; i<Area; i+=CELL)
                for(int j=0; j<Area; j+=CELL)
                    M[i/CELL][j/CELL] = new Pt(sp+j, sp+i);

            // Подгоняем размер изображений под размер ячейки
            Wall = Bitmap.createScaledBitmap(origWall, CELL, CELL, true);
            Arrow = Bitmap.createScaledBitmap(origArrow, CELL, CELL, true);
            Revers = Bitmap.createScaledBitmap(origRevers, CELL, CELL, true);
        }
        // Заливка поверхности рисования лабиринта
        p.setColor(Color.LTGRAY);
        p.setStyle(Paint.Style.FILL);
        canvas.drawRect(M[1][1].x, M[1][1].y, M[N-2][N-2].x+CELL, M[N-2][N-2].y+CELL, p);
        p.setStrokeWidth(2);
        p.setTextSize(40);

        // Отрисовка ячеек игровой поверхности
        for(int i=0; i<N; i++)
            for(int j=0; j<N; j++){
                p.setColor(i == StartPos && j == 0 ? Color.GREEN :
                           i == EndPos && j == N-1 ? Color.BLUE  :
                                                     Color.YELLOW );
                // Рисуем стену
                if(i == 0 || j == 0 || i == N-1 || j == N-1)
                    canvas.drawBitmap(Wall, M[i][j].x, M[i][j].y, null);
                // Рисуем коридоры
                if(M[i][j].cond == 1)
                    canvas.drawRect(M[i][j].x, M[i][j].y, M[i][j].x+CELL, M[i][j].y+CELL, p);
                // Рисуем стрелки направления пути и крестики (путь пройденный дважды)
                if(cond == ConditionType.GAME_SCORE){
                    matrix.setTranslate(M[i][j].x, M[i][j].y);
                    if(M[i][j].path == 2){
                        matrix.postRotate(M[i][j].rotate, M[i][j].x+CELL/2, M[i][j].y+CELL/2);
                        canvas.drawBitmap(Arrow, matrix, null);
                    }
                    if(M[i][j].path == 3) {
                        canvas.drawBitmap(Revers, matrix, null);
                    }
                }
            }
        // Визуальная разметка игрового поля на ячейки
        p.setColor(Color.GRAY);
        for(int i=CELL; i<Area; i+=CELL){
            canvas.drawLine(sp+CELL, sp+i, sp+Area-CELL, sp+i, p);
            canvas.drawLine(sp+i, sp+CELL, sp+i, sp+Area-CELL, p);
        }
        // Отображаем текущие настройки игрового пространства
        p.setColor(Color.BLACK);
        p.setStyle(Paint.Style.FILL);
        canvas.drawRect(0, Height-148, Width, Height-80, p);
        p.setColor(Color.WHITE);
        canvas.drawText("Matrix: "+(N-2)+"х"+(N-2)+", " + "Cell: "+CELL+"px", 100, Height-100, p);

        switch(cond){
            // Если состояние игрового процесса = START_INIT (инициализация)
            case START_INIT:
                // Выбор размера игрового поля. cond изменится при нажатия кнопки "Продолжить"
                path.reset();
                path.moveTo(Width/2, Height-Height*0.4f);
                path.quadTo(Width/2, Height/2, M[N-2][N-3].x-CELL/2, M[N-2][N-3].y);
                p.setColor(Color.RED);
                p.setStrokeWidth(5);
                p.setStyle(Paint.Style.STROKE);
                canvas.drawPath(path, p);
                canvas.drawRoundRect(new RectF(Width/2-300,Height-Height*0.4f,Width/2+505,Height-Height*0.4f+100), 20, 20, p);
                p.setStyle(Paint.Style.FILL);
                p.setColor(Color.YELLOW);
                canvas.drawRect(M[N-3][N-4].x, M[N-3][N-4].y, M[N-3][N-4].x+CELL, M[N-3][N-4].y+CELL, p);
                p.setColor(Color.RED);
                p.setTextSize(50);
                canvas.drawText("Выберите размер игрового поля", Width/2-280, Height-Height*0.4f+65, p);
                break;

            // Выбор стартовой позиции персонажа. Подсвечиваем область выбора
            case START_POSITION:
                if(StartPos != -1) {
                    // Если позиция установлена, меняем состояние игры на END_POSITION
                    cond = ConditionType.END_POSITION;
                    M[StartPos][0].cond = 1;
                }
                else{
                    try {Thread.sleep(30);}
                    catch(InterruptedException e){ }
                    p.setARGB(150, R, G%250, B);
                    canvas.drawRect(M[1][0].x, M[1][0].y, M[N-2][0].x+CELL, M[N-2][0].y+CELL, p);
                    path.reset();
                    path.moveTo(Width/2, Height-Height*0.4f);
                    path.quadTo(Width/2, Width/2, M[N-N/2][1].x, M[N-N/2][1].y);
                    p.setColor(Color.RED);
                    p.setStrokeWidth(5);
                    p.setStyle(Paint.Style.STROKE);
                    canvas.drawPath(path, p);
                    canvas.drawRoundRect(new RectF(Width/2-210,Height-Height*0.4f,Width/2+490,Height-Height*0.4f+100), 20, 20, p);
                    p.setStyle(Paint.Style.FILL);
                    p.setTextSize(50);
                    canvas.drawText("Выберите стартовую ячейку", Width/2f-190, Height-Height*0.4f+65, p);
                    G+=20;
                }
                this.invalidate();
                break;

            // Выбор финишной позиции персонажа. Подсвечиваем область выбора
            case END_POSITION:
                if(EndPos != -1) {
                    // Если позиция установлена, меняем состояние игры на GAME_TIME
                    cond = ConditionType.GAME_TIME;
                    M[EndPos][Area / CELL - 1].cond = 1;
                }
                else{
                    try {Thread.sleep(30);}
                    catch(InterruptedException e){ }
                    p.setARGB(150, R, G%250, B);
                    canvas.drawRect(M[1][N-1].x, M[1][N-1].y,
                                    M[N-2][N-1].x+CELL, M[N-2][N-1].y+CELL, p);
                    path.reset();
                    path.moveTo(Width/2, Height-Height*0.4f);
                    path.quadTo(Width/2, Width/2, M[N-N/3][N-1].x,
                                     M[N-N/3][N-1].y);
                    p.setColor(Color.RED);
                    p.setStrokeWidth(5);
                    p.setStyle(Paint.Style.STROKE);
                    canvas.drawPath(path, p);
                    canvas.drawRoundRect(new RectF(Width/2-210,Height-Height*0.4f,Width/2+500,Height-Height*0.4f+100), 20, 20, p);
                    p.setStyle(Paint.Style.FILL);
                    p.setTextSize(50);
                    canvas.drawText("Выберите финишную ячейку", Width/2f-190, Height-Height*0.4f+65, p);
                    G+=20;
                }
                this.invalidate();
                break;

            // Старт таймера игрового процесса (время на построение лабиринта)
            case GAME_TIME:
                if(Timer == TimeForGame){
                    new Thread(this,"GameTimer").start();
                }
                p.setStyle(Paint.Style.FILL_AND_STROKE);
                p.setColor(Timer > 5 ? Color.MAGENTA : Color.RED);
                p.setTextSize(80);
                canvas.drawText(String.valueOf(Timer), Timer >= 10 ? Width/2-42 : Width/2-22, Width+270, p);
                p.setStyle(Paint.Style.STROKE);
                p.setStrokeWidth(8);

                for(int i = 0; i < Alpha.length; i++){
                    p.setAlpha(Alpha[i]);
                    canvas.drawCircle(Width/2, Width+250, 90 + i * 7, p);
                }
                break;

            // Проверка наличия пути в лабиринте
            case CHECK_PATH:
                Thread t = new Thread(new MoveHero(this), "Start");
                t.start();
                // Ожидаем результат проврки
                try{ t.join(); }
                catch(InterruptedException e){}
                if(PathExists)
                    cond = ConditionType.FINDE_PATH;
                else
                    cond = ConditionType.STOP;
                this.invalidate();
                break;

            // Если путь есть, то персонаж обходит лабиринт
            case FINDE_PATH:
                // Подгоняем размер персонажа под размер ячейки
                RelativeLayout.LayoutParams params = (RelativeLayout.LayoutParams) HERRO.getLayoutParams();
                params.height = CELL;
                params.width = CELL;
                HERRO.setLayoutParams(params);
                // Перенос персонажа на стартовую позицию
                HERRO.setX(M[StartPos][0].x);
                HERRO.setY(M[StartPos][0].y);
                HERRO.setVisibility(0);
                // Начинаем обход лабиринта персонажем
                new Thread(new MoveHero(this), "Start").start();
                break;

            // При обходе лабиринта, считаем очки и выводим результат на экран
            case GAME_SCORE:
                p.setTextSize(50);
                p.setStrokeWidth(5);
                p.setColor(Color.YELLOW);
                p.setStyle(Paint.Style.FILL);
                p.setAlpha(100);
                canvas.drawRoundRect(new RectF(Width/2-130, Height/2+180, Width/2 +
                        (Score/10==0 ? 140 : Score/10<=9 ? 150 : 165), Height/2+285), 20, 20, p);
                p.setColor(Color.RED);
                p.setAlpha(255);
                p.setTypeface(Typeface.DEFAULT_BOLD);
                canvas.drawText("ОЧКИ: " + String.valueOf(Score), Width/2-100, Height/2+250, p);
                p.setStyle(Paint.Style.STROKE);
                canvas.drawRoundRect(new RectF(Width/2-130, Height/2+180, Width/2 +
                        (Score/10==0 ? 140 : Score/10<=9 ? 150 : 165), Height/2+285), 20, 20, p);
                p.setTypeface(Typeface.DEFAULT);
                // Когда персонаж пришел к финишу
                if(FinishGame) {
                    // Если результат лучший, поздравляем игрока
                    if(Score > BestResult && !StopCongret){
                        if(B_pts == null){
                            B_pts = new Balloon[numBall];
                            Random r = new Random();
                            for(int i=0; i<numBall; i++){
                                B_pts[i] = new Balloon(r.nextInt(Width-200)+100,
                                        r.nextInt(Height-Height/2)+Height+100,
                                        BitmapFactory.decodeResource(getResources(), Balls[r.nextInt(5)]));
                            }
                            path.reset();
                            path.addRoundRect(Width/3f-80, 4/5f*Height-240, Width/3f*2+80,
                                              4/5f*Height+70, 100, 100, Path.Direction.CW);
                            // Запускаем поток пересчета координат воздушных шаров
                            new Thread(this, "Congrates").start();
                        }
                        // Отрисовка шаров и надписи "ЛУЧШИЙ РЕЗУЛЬТАТ"
                        p.setARGB(80, R, G%250, B);
                        canvas.save();
                        canvas.scale(scale, scale, Width/2f, 4/5f*Height-180);
                        p.setStyle(Paint.Style.FILL);
                        canvas.drawPath(path, p);
                        p.setStrokeWidth(12);
                        p.setARGB(255, 153, 0, 255);
                        p.setStyle(Paint.Style.STROKE);
                        canvas.drawPath(path, p);
                        p.setTextSize(90);
                        p.setStrokeWidth(5);
                        p.setStyle(Paint.Style.FILL_AND_STROKE);
                        p.setColor(Color.RED);
                        p.setTextAlign(Paint.Align.CENTER);
                        canvas.drawText("ЛУЧШИЙ", Width/2, (int)(4/5.*Height-130), p);
                        canvas.drawText("РЕЗУЛЬТАТ", Width/2, (int)(4/5.*Height), p);
                        canvas.restore();
                        p.setTextAlign(Paint.Align.LEFT);
                        for(int i=0; i<numBall; i++){
                            matrix.setTranslate(B_pts[i].x, B_pts[i].y);
                            canvas.drawBitmap(B_pts[i].btp, matrix, null);
                        }
                        G += 20;
                    }
                    // Включаем видимость кнопок в нижней части экрана
                    else
                        ButtonControl(0);
                }
                break;

            // Если лабиринт не имеет выхода, выводим сообщение пользователю
            case STOP:
                p.setTextSize(50);
                p.setStrokeWidth(5);
                p.setColor(Color.RED);
                p.setStyle(Paint.Style.FILL);
                canvas.drawText("ЛАБИРИНТ НЕ ИМЕЕТ ВЫХОДА!", 180, Height/2+250, p);
                ButtonControl(0);
                break;
        }
    }
}

// Класс для работы с Базой Данных
class DB_Helper extends SQLiteOpenHelper {
    // Создаём базу данных myDB, если она не существует
    public DB_Helper(Context context) {
        super(context, "myDB", null, 1);
    }

    // Сохраняем строку с результатом в таблицу TableResults
    public void Save(SQLiteDatabase db, int Result, String Rules, String DateTime){
        ContentValues cv = new ContentValues();
        cv.put("result", Result);
        cv.put("rules", Rules);
        cv.put("date", DateTime);
        db.insert("TableResults", null, cv);
    }

    public int MaxResult(SQLiteDatabase db){
        Cursor cursor = db.rawQuery("select * from TableResults where result = " +
                "(select max(result) from TableResults)", null);
        cursor.moveToFirst();
        return cursor.getCount()==0 ? 0 : cursor.getInt(cursor.getColumnIndex("result"));
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        // Создаём таблицу TableResults, если она не существует
        db.execSQL("CREATE TABLE TableResults (result integer, rules text, date text);");
    }
    @Override
    public void onUpgrade(SQLiteDatabase db, int oldV, int newV) {}
}
