package final_work.app;

import java.sql.Date;
import java.text.SimpleDateFormat;

// Класс реализует процесс перемещения игрока и проверку наличия
// пути в лабиринте от точки входа к выходу
class MoveHero implements Runnable{
    enum MovingT { gorizontal, vertical }
    DrawView myDraw;

    public MoveHero(DrawView drawView){
        myDraw = drawView;
        // Обнуляем очки
        myDraw.Score = 0;
        // Инициализируем поле path, каждого элемента массива M, единицей
        // Фактически это создание графа обход которого будем совершать
        for(int i=0; i<myDraw.Area/myDraw.CELL; i++)
            for(int j=0; j<myDraw.Area/myDraw.CELL; j++)
                myDraw.M[i][j].path = myDraw.M[i][j].cond;
        myDraw.M[myDraw.StartPos][0].path = 2;
    }

    public void Delay(int Count_ms){
        try {Thread.sleep(Count_ms);}
        catch(InterruptedException e){}
    }

    // Запускаем поток при вызоме метода start() объекта MoveHero
    public void run(){
        switch(DrawView.cond){
            case CHECK_PATH:
                Move(myDraw.StartPos, 0, true);
                break;
            case FINDE_PATH:
                DrawView.cond = ConditionType.GAME_SCORE;
                Delay(500);
                Move(myDraw.StartPos, 0, false);
                Delay(400);
                myDraw.FinishGame = true;
                // Сохраняем результат текущей игры
                myDraw.dbHelper.Save(myDraw.db, myDraw.Score, String.valueOf(myDraw.N-2)+'x'+
                                String.valueOf(myDraw.N-2)+", "+String.valueOf(myDraw.CELL)+"px",
                                new SimpleDateFormat("dd.MM.yy(HH:mm)").
                                format(new Date(System.currentTimeMillis())));
                myDraw.invalidate();
                break;
        }
    }

    // Установка новых координат для перемещения персонажа
    public void StepAndDelay(int coord, MovingT mType){
        switch(mType){
            case gorizontal :
                myDraw.HERRO.setX(coord); break;
            case vertical:
                myDraw.HERRO.setY(coord); break;}
        Delay(3);
    }

    // Установка угла поворота стрелки-указателя пути персонажа
    public void SetRoteta(int X, int Y, int angle){
        myDraw.M[(X-myDraw.sp)/myDraw.CELL][(Y-myDraw.sp)/myDraw.CELL].rotate = angle;
        myDraw.invalidate();
    }

    // Вычисление и установка параметров для перемещения персонажа
    public void DrawHerro(int X, int Y, int dX, int dY){
        myDraw.Score ++;
        int curX = (int)myDraw.HERRO.getX();
        int curY = (int)myDraw.HERRO.getY();

        if(curX < dX) {                          // движемся вперед
            myDraw.HERRO.setRotation(0);
            SetRoteta(Y, X, 0);
            for (int i = curX; i <= curX + myDraw.CELL; i++)
                StepAndDelay(i, MovingT.gorizontal );
        }

        else if(curX > dX) {                     // движемся назад
            myDraw.HERRO.setRotation(180);
            SetRoteta(Y, X, 180);
            for (int i = curX; i >= curX - myDraw.CELL; i--)
                StepAndDelay(i, MovingT.gorizontal );
        }

        else if(curY < dY) {                      // движемся вниз
            myDraw.HERRO.setRotation(90);
            SetRoteta(Y, X, 90);
            for (int i = curY; i <= curY + myDraw.CELL; i++)
                StepAndDelay(i, MovingT.vertical);
        }

        else if(curY > dY) {                      // движемся вверх
            myDraw.HERRO.setRotation(270);
            SetRoteta(Y, X, 270);
            for (int i = curY; i >= curY - myDraw.CELL; i--)
                StepAndDelay(i, MovingT.vertical);
        }
    }

    public void Move(int i, int j, boolean check){
        Pt N = j == myDraw.Area/myDraw.CELL-1 ? new Pt(i,j)   :
                myDraw.M[i][j+1].path == 1    ? new Pt(i,j+1) :
                j == 0                        ? new Pt(i,j)   :
                myDraw.M[i+1][j].path == 1    ? new Pt(i+1,j) :
                myDraw.M[i][j-1].path == 1    ? new Pt(i,j-1) :
                myDraw.M[i-1][j].path == 1    ? new Pt(i-1,j) :
                                                new Pt(i,j);

        if(N.y == 0){
            // Лабиринт не имеет выхода
            myDraw.PathExists = false;
        }
        else if(N.y == myDraw.Area/myDraw.CELL-1){
            // Если достигли финиша
            myDraw.PathExists = true;
            if(!check)
                DrawHerro(myDraw.M[i][j].x, myDraw.M[i][j].y, myDraw.M[myDraw.EndPos][myDraw.Area/myDraw.CELL-1].x,
                        myDraw.M[myDraw.EndPos][myDraw.Area/myDraw.CELL-1].y);
        }
        else {
            // Если нашли не пройденный путь
            if (i != N.x || j != N.y) {
                // Перемещаем персонажа из текущей позиции в найденную
                if(!check) DrawHerro(myDraw.M[i][j].x, myDraw.M[i][j].y, myDraw.M[N.x][N.y].x, myDraw.M[N.x][N.y].y);
                // Отмечаем путь как пройденный единожды
                myDraw.M[N.x][N.y].path = 2;
                // Продолжаем поиск нового пути из позиции в которую перешли
                Move(N.x, N.y, check);
                // Если вернулись по пути пройденному единожды
                if (myDraw.M[N.x][N.y].path == 3) {
                    // Возвращаем персонажа из предыдущей позиции
                    if(!check) DrawHerro(myDraw.M[N.x][N.y].x, myDraw.M[N.x][N.y].y, myDraw.M[i][j].x, myDraw.M[i][j].y);
                    // И пытаемся найти новый путь из ячейки в которую вернулись
                    Move(i, j, check);
                }
            }
            // Если нет не пройденного пути
            else
                // Отмечаем текущую позицию как пройденную дважды
                // и рекурсия вернет нас к предыдущей позиции
                myDraw.M[i][j].path = 3;
        }
    }
}
