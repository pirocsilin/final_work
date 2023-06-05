package final_work.app;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

public class ResultsActivity extends Activity {

    LinearLayout newText;       // Контейнер для очередной строки с результатом
    LinearLayout textPanel;     // Контейнер для группировки всех результптов
    LinearLayout.LayoutParams params; // Параметры для TextView элемента
    TextView text;                    // View элемент результирующей строки
    DB_Helper dbHelper;         // Обьект для упрощения работы с базами данных
    SQLiteDatabase db;          // Обьект для манипуляции базой данных
    final String[] HeaderText = new String[]{"Очки", "Размер поля", "Дата"};

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_results);

        textPanel = findViewById(R.id.TextPanel);
        dbHelper = new DB_Helper(this);
        db = dbHelper.getWritableDatabase();
        // Делаем запрос в базу данных. Получаем все строки с сортировкой по очкам
        Cursor cursor = db.query("TableResults", null, null, null, null, null, "result DESC");

        // Если в таблице есть данные
        if (cursor.moveToFirst()){
            // Создаем заголовочную строку
            CreateNewStroke();
            for(int i=0; i<3; i++)
                AddNewStroke(HeaderText[i], 20, Color.RED, i==0 ? 0.2f : 0.4f);
            textPanel.addView(newText);

            // Вставляем пустую строку для разделения заголовка и результатов
            CreateNewStroke();
            AddNewStroke("", 10, 0, 1);
            textPanel.addView(newText);

            // Получаем индексы столбцов таблицы
            int idx[] = {cursor.getColumnIndex("result"),
                         cursor.getColumnIndex("rules"),
                         cursor.getColumnIndex("date")};
            // Получаем данные из БД
            do{
                CreateNewStroke();
                AddNewStroke(String.valueOf(cursor.getInt(idx[0])), 15, Color.LTGRAY, 0.2f);
                AddNewStroke(String.valueOf(cursor.getString(idx[1])), 15, Color.LTGRAY, 0.4f);
                AddNewStroke(String.valueOf(cursor.getString(idx[2])), 15, Color.LTGRAY, 0.4f);
                textPanel.addView(newText);
            }while(cursor.moveToNext());
        }
        // Если таблица результатов пуста
        else{
            CreateNewStroke();
            AddNewStroke("НЕТ ДАННЫХ", 20, Color.LTGRAY, 1);
            textPanel.addView(newText);
        }
    }
    // Создаем контейнер для очередной строки с результатом.
    public void CreateNewStroke(){
        newText = new LinearLayout(this);
        newText.setOrientation(LinearLayout.HORIZONTAL);
    }
    // Втавляем данные в строку с очередным результатом
    public void AddNewStroke(String data, int TextSize, int Color, float Weight){
        text = new TextView(this);
        text.setText(data);
        text.setTextSize(TextSize);
        text.setTextColor(Color);
        text.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);
        params = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT,
                     LinearLayout.LayoutParams.WRAP_CONTENT, Weight);
        text.setWidth(0);
        newText.addView(text, params);
    }
    // Обработчик нажатий на кнопки
    public void onClick(View view){
        Button button = (Button)view;
        Intent intent;
        switch (button.getText().toString()){
            case "НАЧАТЬ ИГРУ":
                intent = new Intent(this, MainActivity.class);
                startActivity(intent);
                break;
            case "НА ГЛАВНУЮ":
                intent = new Intent(this, StartActivity.class);
                startActivity(intent);
                break;
            case "ОЧИСТИТЬ":
                db.delete("TableResults", null, null);
                CreateNewStroke();
                AddNewStroke("НЕТ ДАННЫХ", 20, Color.LTGRAY, 1);
                textPanel.removeAllViews();
                textPanel.addView(newText);
                break;
        }
    }
}
