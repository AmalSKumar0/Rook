package com.example.rook;

import android.content.Context;
import android.content.res.ColorStateList;
import android.database.Cursor;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.graphics.pdf.PdfDocument;
import android.os.Bundle;
import android.os.Environment;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class CollectionTestResultActivity extends AppCompatActivity {

    private DatabaseHelper dbHelper;
    private OkHttpClient client;
    private long projectId = -1;
    private String collectionName = "Collection";
    private String collectionTemplate = "Empty";

    private TextView tvCollectionName, tvRunTimestamp, tvProgressLabel;
    private ProgressBar runProgressBar;
    private MaterialButton btnDownloadPdf;
    private RecyclerView rvRunResults;

    private View runMetricSuccess, runMetricLatency, runMetricPassed, runMetricTotal;

    private final List<TestResultItem> runResults = new ArrayList<>();
    private RunResultsAdapter adapter;

    // Execution state
    private List<ProjectDetailsActivity.Endpoint> endpointsToTest = new ArrayList<>();
    private int currentTestIndex = 0;
    private int successCount = 0;
    private int failureCount = 0;
    private long totalLatency = 0;
    private boolean isTestingComplete = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_collection_test_result);

        dbHelper = new DatabaseHelper(this);
        client = new OkHttpClient();
        projectId = getIntent().getLongExtra("PROJECT_ID", -1);

        NavigationUtils.setupAppChrome(this, "Collection Test Run", true);

        initViews();
        setupRecyclerView();

        if (projectId != -1) {
            loadCollectionInfo();
            loadEndpointsAndStartRun();
        } else {
            Toast.makeText(this, "Invalid Collection ID", Toast.LENGTH_SHORT).show();
            finish();
        }

        btnDownloadPdf.setOnClickListener(v -> exportPdfReport());
    }

    private void initViews() {
        tvCollectionName = findViewById(R.id.tvCollectionName);
        tvRunTimestamp = findViewById(R.id.tvRunTimestamp);
        tvProgressLabel = findViewById(R.id.tvProgressLabel);
        runProgressBar = findViewById(R.id.runProgressBar);
        btnDownloadPdf = findViewById(R.id.btnDownloadPdf);
        rvRunResults = findViewById(R.id.rvRunResults);

        runMetricSuccess = findViewById(R.id.runMetricSuccess);
        runMetricLatency = findViewById(R.id.runMetricLatency);
        runMetricPassed = findViewById(R.id.runMetricPassed);
        runMetricTotal = findViewById(R.id.runMetricTotal);

        // Bind initial empty metrics using custom soft-brutalist layouts
        bindMetric(runMetricSuccess, "SUCCESS RATE", "-- %", "Passed ratio");
        bindMetric(runMetricLatency, "AVG LATENCY", "-- ms", "Mean response");
        bindMetric(runMetricPassed, "PASSED", "0", "0 failures");
        bindMetric(runMetricTotal, "TOTAL RUN", "0", "Endpoints tested");
    }

    private void bindMetric(View root, String label, String value, String subtext) {
        if (root == null) return;
        ((TextView) root.findViewById(R.id.tvMetricLabel)).setText(label);
        ((TextView) root.findViewById(R.id.tvMetricValue)).setText(value);
        ((TextView) root.findViewById(R.id.tvMetricSubtext)).setText(subtext);
    }

    private void setupRecyclerView() {
        rvRunResults.setLayoutManager(new LinearLayoutManager(this));
        adapter = new RunResultsAdapter(runResults);
        rvRunResults.setAdapter(adapter);
    }

    private void loadCollectionInfo() {
        Cursor cursor = dbHelper.getProject(projectId);
        if (cursor != null) {
            if (cursor.moveToFirst()) {
                collectionName = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.KEY_PROJECT_NAME));
                collectionTemplate = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.KEY_PROJECT_TEMPLATE));
                if (TextUtils.isEmpty(collectionTemplate)) {
                    collectionTemplate = "Empty";
                }
                tvCollectionName.setText(collectionName);
                TextView headerTitle = findViewById(R.id.headerTitle);
                if (headerTitle != null) {
                    headerTitle.setText(collectionName + " Run");
                }
            }
            cursor.close();
        }
        SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, yyyy h:mm a", Locale.getDefault());
        tvRunTimestamp.setText("Run started: " + sdf.format(new Date()));
    }

    private void loadEndpointsAndStartRun() {
        AppExecutors.getInstance().diskIO().execute(() -> {
            List<ProjectDetailsActivity.Endpoint> items = new ArrayList<>();
            Cursor cursor = dbHelper.getEndpointsForProject(projectId);
            if (cursor != null) {
                while (cursor.moveToNext()) {
                    long id = cursor.getLong(cursor.getColumnIndexOrThrow(DatabaseHelper.KEY_ID));
                    String method = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.KEY_ENDPOINT_METHOD));
                    String path = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.KEY_ENDPOINT_PATH));
                    String desc = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.KEY_ENDPOINT_DESCRIPTION));
                    String url = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.KEY_ENDPOINT_URL));
                    String headers = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.KEY_ENDPOINT_HEADERS));
                    String body = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.KEY_ENDPOINT_BODY));
                    String authType = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.KEY_ENDPOINT_AUTH_TYPE));
                    String authToken = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.KEY_ENDPOINT_AUTH_TOKEN));
                    String authUsername = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.KEY_ENDPOINT_AUTH_USERNAME));
                    String authPassword = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.KEY_ENDPOINT_AUTH_PASSWORD));
                    items.add(new ProjectDetailsActivity.Endpoint(id, method, path, desc, url, headers, body, authType, authToken, authUsername, authPassword));
                }
                cursor.close();
            }

            AppExecutors.getInstance().mainThread().execute(() -> {
                endpointsToTest = items;
                if (endpointsToTest.isEmpty()) {
                    tvProgressLabel.setText("No APIs to test in this collection.");
                    runProgressBar.setProgress(100);
                    return;
                }
                runProgressBar.setMax(endpointsToTest.size());
                tvProgressLabel.setText("Preparing test execution for " + endpointsToTest.size() + " endpoints...");
                
                // Initialize list with pending items
                for (ProjectDetailsActivity.Endpoint e : endpointsToTest) {
                    runResults.add(new TestResultItem(e.method, e.path, e.url, "PENDING", 0, "Waiting...", "--"));
                }
                adapter.notifyDataSetChanged();
                
                // Start testing first endpoint
                executeNextTest();
            });
        });
    }

    private void executeNextTest() {
        if (currentTestIndex >= endpointsToTest.size()) {
            finishRun();
            return;
        }

        ProjectDetailsActivity.Endpoint endpoint = endpointsToTest.get(currentTestIndex);
        tvProgressLabel.setText("Testing " + (currentTestIndex + 1) + " of " + endpointsToTest.size() + ": " + endpoint.path);
        runProgressBar.setProgress(currentTestIndex + 1);

        String requestUrl = normalizeUrl(endpoint.url);
        if (TextUtils.isEmpty(requestUrl)) {
            updateTestStatus(currentTestIndex, "FAILURE", 0, "Missing request URL", 0);
            recordDbResult(endpoint, "", "Missing request URL", 0, 0, "FAILURE");
            currentTestIndex++;
            executeNextTest();
            return;
        }

        Request.Builder builder = new Request.Builder().url(requestUrl);
        applyHeaders(builder, endpoint.headers);
        applyAuth(builder, endpoint);

        String method = endpoint.method.toUpperCase();
        String body = endpoint.body != null ? endpoint.body : "";
        RequestBody requestBody = RequestBody.create(body, MediaType.parse("application/json; charset=utf-8"));
        
        switch (method) {
            case "POST":
                builder.post(requestBody);
                break;
            case "PUT":
                builder.put(requestBody);
                break;
            case "DELETE":
                if (body.isEmpty()) {
                    builder.delete();
                } else {
                    builder.delete(requestBody);
                }
                break;
            default:
                builder.get();
                break;
        }

        long startedAt = System.currentTimeMillis();
        client.newCall(builder.build()).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                long duration = System.currentTimeMillis() - startedAt;
                runOnUiThread(() -> {
                    updateTestStatus(currentTestIndex, "FAILURE", (int) duration, e.getMessage(), 0);
                    recordDbResult(endpoint, requestUrl, e.getMessage(), 0, (int) duration, "FAILURE");
                    currentTestIndex++;
                    executeNextTest();
                });
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                long duration = System.currentTimeMillis() - startedAt;
                int code = response.code();
                String responseBody = response.body() != null ? response.body().string() : "";
                String resultStatus = (code >= 200 && code < 300) ? "SUCCESS" : "FAILURE";
                String message = response.message();

                runOnUiThread(() -> {
                    updateTestStatus(currentTestIndex, resultStatus, (int) duration, responseBody, code);
                    recordDbResult(endpoint, requestUrl, responseBody, code, (int) duration, resultStatus);
                    currentTestIndex++;
                    executeNextTest();
                });
            }
        });
    }

    private void updateTestStatus(int index, String status, int latency, String responseBody, int statusCode) {
        TestResultItem item = runResults.get(index);
        item.status = status;
        item.latency = latency;
        item.responseBody = responseBody;
        item.statusCode = statusCode == 0 ? "ERROR" : String.valueOf(statusCode);
        
        if ("SUCCESS".equals(status)) {
            successCount++;
        } else {
            failureCount++;
        }
        totalLatency += latency;

        adapter.notifyItemChanged(index);
        updateMetricsUI();
    }

    private void recordDbResult(ProjectDetailsActivity.Endpoint endpoint, String requestUrl, String responseBody, int statusCode, int responseTime, String resultStatus) {
        long timestamp = System.currentTimeMillis();
        AppExecutors.getInstance().diskIO().execute(() -> {
            dbHelper.addTestResult(endpoint.id, projectId, requestUrl, endpoint.method, endpoint.headers, endpoint.body,
                    responseBody, statusCode, responseTime, resultStatus, timestamp);
            dbHelper.addHistory(endpoint.method, endpoint.path, statusCode > 0 ? statusCode + " OK" : "ERROR", responseTime, responseBody);
        });
    }

    private void updateMetricsUI() {
        int tested = successCount + failureCount;
        float rate = tested > 0 ? ((float) successCount / tested) * 100f : 0f;
        long avg = tested > 0 ? totalLatency / tested : 0;

        bindMetric(runMetricSuccess, "SUCCESS RATE", String.format(Locale.US, "%.1f%%", rate), "Passed ratio");
        bindMetric(runMetricLatency, "AVG LATENCY", avg + " ms", "Mean response");
        bindMetric(runMetricPassed, "PASSED", String.valueOf(successCount), failureCount + " failures");
        bindMetric(runMetricTotal, "TOTAL RUN", String.valueOf(tested), "Endpoints tested");
    }

    private void finishRun() {
        isTestingComplete = true;
        tvProgressLabel.setText("Run Complete! " + successCount + " passed, " + failureCount + " failed.");
        btnDownloadPdf.setEnabled(true);
        Toast.makeText(this, "Collection verification finished.", Toast.LENGTH_SHORT).show();
    }

    private String normalizeUrl(String url) {
        if (url == null) return "";
        String trimmed = url.trim();
        if (!trimmed.startsWith("http://") && !trimmed.startsWith("https://")) {
            return "https://" + trimmed;
        }
        return trimmed;
    }

    private void applyHeaders(Request.Builder builder, String headers) {
        if (TextUtils.isEmpty(headers)) return;
        String[] lines = headers.split("\\n");
        for (String line : lines) {
            int separator = line.indexOf(':');
            if (separator <= 0) continue;
            String name = line.substring(0, separator).trim();
            String value = line.substring(separator + 1).trim();
            if (!name.isEmpty() && !value.isEmpty()) {
                builder.addHeader(name, value);
            }
        }
    }

    private void applyAuth(Request.Builder builder, ProjectDetailsActivity.Endpoint endpoint) {
        if ("Bearer Token".equalsIgnoreCase(endpoint.authType) && !TextUtils.isEmpty(endpoint.authToken)) {
            builder.header("Authorization", "Bearer " + endpoint.authToken);
        } else if ("Basic Auth".equalsIgnoreCase(endpoint.authType)) {
            String credentials = endpoint.authUsername + ":" + endpoint.authPassword;
            String auth = "Basic " + android.util.Base64.encodeToString(credentials.getBytes(), android.util.Base64.NO_WRAP);
            builder.header("Authorization", auth);
        }
    }

    private void exportPdfReport() {
        if (!isTestingComplete) {
            Toast.makeText(this, "Testing is still in progress.", Toast.LENGTH_SHORT).show();
            return;
        }

        PdfDocument document = new PdfDocument();
        int pageWidth = 595; // A4 Width in points
        int pageHeight = 842; // A4 Height in points
        int pageNumber = 1;

        PdfDocument.PageInfo pageInfo = new PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create();
        PdfDocument.Page page = document.startPage(pageInfo);
        Canvas canvas = page.getCanvas();

        Paint paint = new Paint();
        paint.setAntiAlias(true);

        // Notion Design System Colors for PDF
        int darkText = Color.parseColor("#1a1a1a");     // notion_ink
        int subText = Color.parseColor("#5d5b54");      // notion_slate
        int primaryGreen = Color.parseColor("#1aae39"); // notion_brand_green
        int lightGreenBg = Color.parseColor("#d9f3e1"); // notion_card_tint_mint
        int strokeColor = Color.parseColor("#c8c4be");  // notion_hairline_strong
        int redColor = Color.parseColor("#e03131");     // Semantic error

        int y = 40;

        // Draw Soft-Brutalist Title Banner
        paint.setColor(primaryGreen);
        canvas.drawRect(30, y, pageWidth - 30, y + 60, paint);
        paint.setStyle(Paint.Style.STROKE);
        paint.setColor(strokeColor);
        paint.setStrokeWidth(3);
        canvas.drawRect(30, y, pageWidth - 30, y + 60, paint);

        paint.setStyle(Paint.Style.FILL);
        paint.setColor(Color.WHITE);
        paint.setTextSize(22);
        paint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
        canvas.drawText("ROOK API VERIFICATION REPORT", 50, y + 38, paint);

        y += 85;

        // Report Metadata
        paint.setColor(darkText);
        paint.setTextSize(12);
        paint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.NORMAL));
        
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
        String dateStr = sdf.format(new Date());

        canvas.drawText("Collection: " + collectionName + " (" + collectionTemplate + ")", 35, y, paint);
        canvas.drawText("Executed On: " + dateStr, 35, y + 18, paint);

        y += 40;

        // Notion-style Summary Cards Grid (surface + hairline)
        // Success Rate Card
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(Color.parseColor("#f6f5f4")); // notion_surface
        canvas.drawRect(35, y, 195, y + 70, paint);
        paint.setStyle(Paint.Style.STROKE);
        paint.setColor(strokeColor);
        paint.setStrokeWidth(1);
        canvas.drawRect(35, y, 195, y + 70, paint);

        // Average Latency Card
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(Color.parseColor("#f6f5f4")); // notion_surface
        canvas.drawRect(210, y, 370, y + 70, paint);
        paint.setStyle(Paint.Style.STROKE);
        paint.setColor(strokeColor);
        canvas.drawRect(210, y, 370, y + 70, paint);

        // Verification Passed/Total Card
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(Color.parseColor("#f6f5f4")); // notion_surface
        canvas.drawRect(385, y, 545, y + 70, paint);
        paint.setStyle(Paint.Style.STROKE);
        paint.setColor(strokeColor);
        canvas.drawRect(385, y, 545, y + 70, paint);

        // Paint summary texts
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(subText);
        paint.setTextSize(10);
        paint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
        canvas.drawText("SUCCESS RATE", 45, y + 20, paint);
        canvas.drawText("AVG LATENCY", 220, y + 20, paint);
        canvas.drawText("PASSED / RUN", 395, y + 20, paint);

        int tested = successCount + failureCount;
        float successRate = tested > 0 ? ((float) successCount / tested) * 100f : 0f;
        long avgLatency = tested > 0 ? totalLatency / tested : 0;

        paint.setColor(darkText);
        paint.setTextSize(20);
        canvas.drawText(String.format(Locale.US, "%.1f%%", successRate), 45, y + 45, paint);
        canvas.drawText(avgLatency + " ms", 220, y + 45, paint);
        canvas.drawText(successCount + " / " + tested, 395, y + 45, paint);

        paint.setColor(subText);
        paint.setTextSize(9);
        paint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.NORMAL));
        canvas.drawText("Test success ratio", 45, y + 60, paint);
        canvas.drawText("Mean response time", 220, y + 60, paint);
        canvas.drawText(failureCount + " failures logged", 395, y + 60, paint);

        y += 105;

        // Table Header
        paint.setColor(darkText);
        paint.setTextSize(13);
        paint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
        canvas.drawText("Endpoint Verification Details", 35, y, paint);

        y += 15;

        // Draw Table Header Borders (Notion hairline style)
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(Color.parseColor("#f0eeec")); // notion_card_tint_gray
        canvas.drawRect(35, y, pageWidth - 35, y + 25, paint);
        paint.setStyle(Paint.Style.STROKE);
        paint.setColor(strokeColor);
        paint.setStrokeWidth(1);
        canvas.drawRect(35, y, pageWidth - 35, y + 25, paint);

        paint.setStyle(Paint.Style.FILL);
        paint.setColor(darkText);
        paint.setTextSize(10);
        canvas.drawText("METHOD", 42, y + 17, paint);
        canvas.drawText("API PATH / ROUTE", 102, y + 17, paint);
        canvas.drawText("STATUS", 392, y + 17, paint);
        canvas.drawText("LATENCY", 482, y + 17, paint);

        y += 25;

        // Rows
        paint.setTypeface(Typeface.create(Typeface.MONOSPACE, Typeface.NORMAL));
        for (int i = 0; i < runResults.size(); i++) {
            TestResultItem item = runResults.get(i);

            // Handle page split if y position goes too close to bottom
            if (y > pageHeight - 60) {
                document.finishPage(page);
                pageNumber++;
                pageInfo = new PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create();
                page = document.startPage(pageInfo);
                canvas = page.getCanvas();
                
                y = 40;
                // Re-draw table header
                paint.setStyle(Paint.Style.FILL);
                paint.setColor(Color.parseColor("#f0eeec")); // notion_card_tint_gray
                canvas.drawRect(35, y, pageWidth - 35, y + 25, paint);
                paint.setStyle(Paint.Style.STROKE);
                paint.setColor(strokeColor);
                canvas.drawRect(35, y, pageWidth - 35, y + 25, paint);

                paint.setStyle(Paint.Style.FILL);
                paint.setColor(darkText);
                paint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
                canvas.drawText("METHOD", 42, y + 17, paint);
                canvas.drawText("API PATH / ROUTE", 102, y + 17, paint);
                canvas.drawText("STATUS", 392, y + 17, paint);
                canvas.drawText("LATENCY", 482, y + 17, paint);
                
                y += 25;
                paint.setTypeface(Typeface.create(Typeface.MONOSPACE, Typeface.NORMAL));
            }

            // Draw Row Box
            paint.setStyle(Paint.Style.STROKE);
            paint.setColor(strokeColor);
            paint.setStrokeWidth(1);
            canvas.drawRect(35, y, pageWidth - 35, y + 30, paint);

            // Method Badge
            paint.setStyle(Paint.Style.FILL);
            // Notion method colors: GET=green, POST=purple-800, PUT=orange-deep, DELETE=error
            paint.setColor("GET".equalsIgnoreCase(item.method) ? primaryGreen : ("POST".equalsIgnoreCase(item.method) ? Color.parseColor("#391c57") : ("PUT".equalsIgnoreCase(item.method) ? Color.parseColor("#793400") : redColor)));
            paint.setTextSize(9);
            paint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
            canvas.drawText(item.method, 42, y + 18, paint);

            // Path
            paint.setColor(darkText);
            paint.setTypeface(Typeface.create(Typeface.MONOSPACE, Typeface.NORMAL));
            String path = item.path;
            if (path.length() > 32) {
                path = path.substring(0, 29) + "...";
            }
            canvas.drawText(path, 102, y + 18, paint);

            // Status Badge
            if ("SUCCESS".equalsIgnoreCase(item.status)) {
                paint.setColor(primaryGreen);
                paint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
                canvas.drawText("SUCCESS (" + item.statusCode + ")", 392, y + 18, paint);
            } else {
                paint.setColor(redColor);
                paint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
                canvas.drawText("FAIL (" + item.statusCode + ")", 392, y + 18, paint);
            }

            // Latency
            paint.setColor(subText);
            paint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.NORMAL));
            canvas.drawText(item.latency + " ms", 482, y + 18, paint);

            y += 30;
        }

        // Draw Footer
        y = pageHeight - 35;
        paint.setColor(subText);
        paint.setTextSize(8);
        paint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.ITALIC));
        canvas.drawText("Generated by Rook API Tester. Page " + pageNumber, 35, y, paint);

        document.finishPage(page);

        // Save file
        String safeName = collectionName.replaceAll("[^a-zA-Z0-9_-]", "_");
        String filename = "Rook_Report_" + safeName + "_" + System.currentTimeMillis() + ".pdf";

        File pdfFile = null;
        try {
            // Try saving to public Downloads first
            File publicDownloads = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
            if (publicDownloads != null && publicDownloads.exists()) {
                pdfFile = new File(publicDownloads, filename);
            } else {
                // Fallback to app-specific external downloads directory
                pdfFile = new File(getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS), filename);
            }
            
            FileOutputStream fos = new FileOutputStream(pdfFile);
            document.writeTo(fos);
            document.close();
            fos.close();

            showExportSuccessDialog(pdfFile.getAbsolutePath());
        } catch (Exception e) {
            document.close();
            Toast.makeText(this, "Failed to save PDF: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private void showExportSuccessDialog(String path) {
        new MaterialAlertDialogBuilder(this)
                .setTitle("Report Downloaded")
                .setMessage("Your PDF analytics report has been downloaded successfully.\n\nSaved at:\n" + path)
                .setPositiveButton("Dismiss", null)
                .show();
    }

    // Model class for RecyclerView
    private static class TestResultItem {
        String method;
        String path;
        String url;
        String status;
        int latency;
        String responseBody;
        String statusCode;

        TestResultItem(String method, String path, String url, String status, int latency, String responseBody, String statusCode) {
            this.method = method;
            this.path = path;
            this.url = url;
            this.status = status;
            this.latency = latency;
            this.responseBody = responseBody;
            this.statusCode = statusCode;
        }
    }

    // RecyclerView Adapter
    private static class RunResultsAdapter extends RecyclerView.Adapter<RunResultsAdapter.ViewHolder> {
        private final List<TestResultItem> items;

        RunResultsAdapter(List<TestResultItem> items) {
            this.items = items;
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_test_result_card, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            TestResultItem item = items.get(position);
            holder.tvPath.setText(item.path);
            holder.tvUrl.setText(item.url);
            holder.tvMethodBadge.setText(item.method);
            holder.tvLatency.setText(item.latency + "ms");
            holder.tvStatusBadge.setText(item.status);

            // Handle preview
            if ("Waiting...".equals(item.responseBody) || "Executing request...".equals(item.responseBody)) {
                holder.tvResponsePreview.setText(item.responseBody);
            } else if (item.responseBody != null && !item.responseBody.isEmpty()) {
                String clean = item.responseBody.trim();
                try {
                    if (clean.startsWith("{")) {
                        holder.tvResponsePreview.setText(new org.json.JSONObject(clean).toString(2));
                    } else if (clean.startsWith("[")) {
                        holder.tvResponsePreview.setText(new org.json.JSONArray(clean).toString(2));
                    } else {
                        holder.tvResponsePreview.setText(item.responseBody);
                    }
                } catch (Exception ignored) {
                    holder.tvResponsePreview.setText(item.responseBody);
                }
            } else {
                holder.tvResponsePreview.setText("(No response body content)");
            }

            // Notion Design System – result status accent + badge
            int accentColor = Color.parseColor("#f0eeec"); // card_tint_gray placeholder
            int badgeBg = R.drawable.bg_badge_neutral;
            int badgeTextColor = Color.parseColor("#5d5b54"); // notion_slate

            if ("SUCCESS".equals(item.status)) {
                accentColor = Color.parseColor("#d9f3e1"); // card_tint_mint
                badgeBg = R.drawable.bg_badge_get;
                badgeTextColor = Color.parseColor("#1aae39"); // brand_green
            } else if ("FAILURE".equals(item.status)) {
                accentColor = Color.parseColor("#fde0ec"); // card_tint_rose
                badgeBg = R.drawable.bg_badge_delete;
                badgeTextColor = Color.parseColor("#e03131"); // semantic error
            }

            holder.vAccentStripe.setBackgroundColor(accentColor);
            holder.tvStatusBadge.setBackgroundResource(badgeBg);
            holder.tvStatusBadge.setTextColor(badgeTextColor);

            // Notion Design System – method badge style
            int mBadgeBg = R.drawable.bg_badge_neutral;
            int mBadgeText = Color.parseColor("#5d5b54"); // notion_slate
            switch (item.method.toUpperCase()) {
                case "GET":
                    mBadgeBg = R.drawable.bg_badge_get;
                    mBadgeText = Color.parseColor("#1aae39"); // brand_green
                    break;
                case "POST":
                    mBadgeBg = R.drawable.bg_badge_post;
                    mBadgeText = Color.parseColor("#391c57"); // brand_purple_800
                    break;
                case "PUT":
                    mBadgeBg = R.drawable.bg_badge_put;
                    mBadgeText = Color.parseColor("#793400"); // brand_orange_deep
                    break;
                case "DELETE":
                    mBadgeBg = R.drawable.bg_badge_delete;
                    mBadgeText = Color.parseColor("#e03131"); // semantic error
                    break;
            }
            holder.tvMethodBadge.setBackgroundResource(mBadgeBg);
            holder.tvMethodBadge.setTextColor(mBadgeText);
        }

        @Override
        public int getItemCount() {
            return items.size();
        }

        static class ViewHolder extends RecyclerView.ViewHolder {
            TextView tvMethodBadge, tvLatency, tvStatusBadge, tvPath, tvUrl, tvResponsePreview;
            View vAccentStripe;

            ViewHolder(@NonNull View itemView) {
                super(itemView);
                tvMethodBadge = itemView.findViewById(R.id.tvMethodBadge);
                tvLatency = itemView.findViewById(R.id.tvLatency);
                tvStatusBadge = itemView.findViewById(R.id.tvStatusBadge);
                tvPath = itemView.findViewById(R.id.tvPath);
                tvUrl = itemView.findViewById(R.id.tvUrl);
                tvResponsePreview = itemView.findViewById(R.id.tvResponsePreview);
                vAccentStripe = itemView.findViewById(R.id.vAccentStripe);
            }
        }
    }
}
