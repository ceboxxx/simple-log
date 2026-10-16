package com.ceboxxx.simplelog;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.provider.Settings;
import android.view.View;
import android.view.WindowInsets;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public final class MainActivity extends Activity {
    private final ExecutorService worker = Executors.newSingleThreadExecutor();
    private TextView status;
    private Button permission, refresh, export;
    private ArrayAdapter<String> adapter;
    private boolean loading;
    private int dp(int n) { return Math.round(n * getResources().getDisplayMetrics().density); }
    @Override public void onCreate(Bundle saved) {
        super.onCreate(saved);
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(Color.rgb(250,250,250));
        root.setPadding(dp(20), dp(16), dp(20), 0);
        setContentView(root);
        root.setOnApplyWindowInsetsListener((v, insets) -> {
            if (android.os.Build.VERSION.SDK_INT >= 30) {
                android.graphics.Insets bars = insets.getInsets(WindowInsets.Type.systemBars() | WindowInsets.Type.displayCutout());
                v.setPadding(dp(20) + bars.left, dp(16) + bars.top, dp(20) + bars.right, bars.bottom);
            } else {
                v.setPadding(dp(20) + insets.getSystemWindowInsetLeft(), dp(16) + insets.getSystemWindowInsetTop(), dp(20) + insets.getSystemWindowInsetRight(), insets.getSystemWindowInsetBottom());
            }
            return insets;
        });
        TextView title = new TextView(this);
        title.setText("Simple Log"); title.setTextSize(28); title.setTextColor(Color.rgb(20,30,45));
        root.addView(title);
        TextView subtitle = new TextView(this);
        subtitle.setText("Historial de aplicaciones · Solo en este móvil");
        subtitle.setPadding(0, dp(6), 0, dp(12)); root.addView(subtitle);
        status = new TextView(this); root.addView(status);
        permission = new Button(this); permission.setText("Dar acceso a datos de uso");
        permission.setOnClickListener(v -> startActivity(new Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)));
        root.addView(permission);
        LinearLayout actions = new LinearLayout(this);
        refresh = new Button(this); refresh.setText("Actualizar"); refresh.setOnClickListener(v -> load());
        export = new Button(this); export.setText("Exportar TXT");
        export.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT).setType("text/plain")
                .addCategory(Intent.CATEGORY_OPENABLE).putExtra(Intent.EXTRA_TITLE, "simple-log.txt");
            startActivityForResult(intent, 10);
        });
        actions.addView(refresh, new LinearLayout.LayoutParams(0, -2, 1));
        actions.addView(export, new LinearLayout.LayoutParams(0, -2, 1)); root.addView(actions);
        ListView list = new ListView(this);
        adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1);
        list.setAdapter(adapter); root.addView(list, new LinearLayout.LayoutParams(-1, 0, 1));
        TextView note = new TextView(this);
        note.setText("Últimas 1.000 entradas; el TXT incluye todas. Recogida periódica sujeta al ahorro de batería. Abre la app tras reiniciar y cada pocos días.");
        note.setTextSize(12); note.setPadding(0, dp(8), 0, dp(12)); root.addView(note);
    }
    @Override public void onResume() { super.onResume(); load(); }
    private void load() {
        boolean access = LogStore.hasAccess(this);
        permission.setVisibility(access ? View.GONE : View.VISIBLE);
        if (access) CollectJob.schedule(this);
        if (loading) return;
        loading = true; refresh.setEnabled(false); export.setEnabled(false);
        status.setText(access ? "Actualizando historial…" : "Activa el acceso para registrar las aperturas. El historial guardado sigue disponible.");
        worker.submit(() -> {
            try {
                LogStore.collect(getApplicationContext());
                List<String> lines;
                try (LogStore store = new LogStore(getApplicationContext())) { lines = store.recent(); }
                runOnUiThread(() -> {
                    if (isDestroyed()) return;
                    adapter.clear(); adapter.addAll(lines);
                    if (access) status.setText(lines.isEmpty() ? "Todavía no hay entradas. Abre otra app y vuelve a actualizar." : "Más recientes primero · " + lines.size() + " entradas mostradas");
                    finishLoad();
                });
            } catch (RuntimeException error) {
                runOnUiThread(() -> { if (!isDestroyed()) { status.setText("No se pudo leer el historial. Comprueba el acceso y vuelve a actualizar."); finishLoad(); } });
            }
        });
    }
    private void finishLoad() { loading = false; refresh.setEnabled(true); export.setEnabled(true); }
    @Override protected void onActivityResult(int request, int result, Intent data) {
        super.onActivityResult(request, result, data);
        if (request != 10 || result != RESULT_OK || data == null || data.getData() == null) return;
        android.net.Uri uri = data.getData();
        worker.submit(() -> {
            try {
                LogStore.collect(getApplicationContext());
                try (LogStore store = new LogStore(getApplicationContext());
                     OutputStream out = getContentResolver().openOutputStream(uri, "wt")) {
                    if (out == null) throw new java.io.IOException("No output stream");
                    try (OutputStreamWriter writer = new OutputStreamWriter(out, StandardCharsets.UTF_8)) { store.export(writer); }
                }
                runOnUiThread(() -> Toast.makeText(this, "Historial exportado", Toast.LENGTH_SHORT).show());
            } catch (Exception error) {
                runOnUiThread(() -> Toast.makeText(this, "No se pudo exportar el historial", Toast.LENGTH_LONG).show());
            }
        });
    }
    @Override public void onDestroy() { worker.shutdown(); super.onDestroy(); }
}
