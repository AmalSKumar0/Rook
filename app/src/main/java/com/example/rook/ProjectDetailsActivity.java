package com.example.rook;

import android.content.Intent;
import android.database.Cursor;
import android.graphics.Color;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.text.format.DateUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.textfield.TextInputEditText;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class ProjectDetailsActivity extends AppCompatActivity {

    private DatabaseHelper dbHelper;
    private OkHttpClient client;
    private long projectId = -1;
    private TextView tvProjectTitle, tvProjectSubtext, tvCollectionMeta;
    private RecyclerView rvEndpoints;
    private EndpointsAdapter adapter;
    private final List<Endpoint> endpointList = new ArrayList<>();
    private final List<Endpoint> filteredList = new ArrayList<>();
    private TextInputEditText etSearch;
    private View emptyApiState;
    private String collectionName = "";
    private String collectionDescription = "";
    private String collectionTemplate = "Empty";
    private long collectionCreatedAt = 0L;
    private MaterialButton btnTestCollection;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_project_details);

        dbHelper = new DatabaseHelper(this);
        client = new OkHttpClient();
        projectId = getIntent().getLongExtra("PROJECT_ID", -1);

        tvProjectTitle = findViewById(R.id.tvProjectTitle);
        tvProjectSubtext = findViewById(R.id.tvProjectSubtext);
        tvCollectionMeta = findViewById(R.id.tvCollectionMeta);
        etSearch = findViewById(R.id.etSearch);
        rvEndpoints = findViewById(R.id.rvEndpoints);
        emptyApiState = findViewById(R.id.emptyApiState);

        rvEndpoints.setLayoutManager(new LinearLayoutManager(this));
        adapter = new EndpointsAdapter(filteredList, endpoint -> {
            Intent intent = new Intent(ProjectDetailsActivity.this, ApiLabActivity.class);
            intent.putExtra("API_ID", endpoint.id);
            intent.putExtra("PROJECT_ID", projectId);
            startActivity(intent);
        }, endpoint -> {
            new MaterialAlertDialogBuilder(this)
                    .setTitle("Delete API")
                    .setMessage("Are you sure you want to delete this API endpoint?")
                    .setPositiveButton("Delete", (dialog, which) -> {
                        AppExecutors.getInstance().diskIO().execute(() -> {
                            dbHelper.deleteEndpoint(endpoint.id);
                            AppExecutors.getInstance().mainThread().execute(() -> {
                                loadEndpoints();
                                Toast.makeText(this, "API deleted", Toast.LENGTH_SHORT).show();
                            });
                        });
                    })
                    .setNegativeButton("Cancel", null)
                    .show();
        });
        rvEndpoints.setAdapter(adapter);

        btnTestCollection = findViewById(R.id.btnTestCollection);
        if (btnTestCollection != null) {
            btnTestCollection.setOnClickListener(v -> testEntireCollection());
        }

        MaterialButton btnAddApi = findViewById(R.id.btnAddApi);
        if (btnAddApi != null) {
            btnAddApi.setOnClickListener(v -> {
                Intent intent = new Intent(this, ApiLabActivity.class);
                intent.putExtra("PROJECT_ID", projectId);
                startActivity(intent);
            });
        }

        MaterialButton btnEmptyAddApi = findViewById(R.id.btnEmptyAddApi);
        if (btnEmptyAddApi != null) {
            btnEmptyAddApi.setOnClickListener(v -> {
                Intent intent = new Intent(this, ApiLabActivity.class);
                intent.putExtra("PROJECT_ID", projectId);
                startActivity(intent);
            });
        }

        MaterialButton btnEditCollection = findViewById(R.id.btnEditCollection);
        if (btnEditCollection != null) {
            btnEditCollection.setOnClickListener(v -> showEditCollectionDialog());
        }

        FloatingActionButton fabAddEndpoint = findViewById(R.id.fabAddEndpoint);
        if (fabAddEndpoint != null) {
            fabAddEndpoint.setOnClickListener(v -> {
                Intent intent = new Intent(this, ApiLabActivity.class);
                intent.putExtra("PROJECT_ID", projectId);
                startActivity(intent);
            });
        }

        etSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                filterEndpoints(s.toString());
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        NavigationUtils.setupAppChrome(this, "Collection Details", true);
        loadProjectDetails();
    }

    private void loadProjectDetails() {
        if (projectId == -1) {
            Toast.makeText(this, "Collection not found.", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        Cursor pCursor = dbHelper.getProject(projectId);
        if (pCursor != null) {
            if (pCursor.moveToFirst()) {
                collectionName = pCursor.getString(pCursor.getColumnIndexOrThrow(DatabaseHelper.KEY_PROJECT_NAME));
                collectionDescription = pCursor.getString(pCursor.getColumnIndexOrThrow(DatabaseHelper.KEY_PROJECT_DESCRIPTION));
                collectionTemplate = pCursor.getString(pCursor.getColumnIndexOrThrow(DatabaseHelper.KEY_PROJECT_TEMPLATE));
                collectionCreatedAt = pCursor.getLong(pCursor.getColumnIndexOrThrow(DatabaseHelper.KEY_PROJECT_CREATED_AT));

                if (TextUtils.isEmpty(collectionTemplate)) {
                    collectionTemplate = "Empty";
                }
                bindCollectionHeader();
            } else {
                Toast.makeText(this, "Collection not found.", Toast.LENGTH_SHORT).show();
                finish();
                return;
            }
            pCursor.close();
        }

        loadEndpoints();
    }

    private void loadEndpoints() {
        AppExecutors.getInstance().diskIO().execute(() -> {
            List<Endpoint> newItems = new ArrayList<>();
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
                    newItems.add(new Endpoint(id, method, path, desc, url, headers, body, authType, authToken, authUsername, authPassword));
                }
                cursor.close();
            }
            AppExecutors.getInstance().mainThread().execute(() -> {
                endpointList.clear();
                endpointList.addAll(newItems);
                filterEndpoints(etSearch.getText() != null ? etSearch.getText().toString() : "");
            });
        });
    }

    private void filterEndpoints(String query) {
        filteredList.clear();
        if (TextUtils.isEmpty(query)) {
            filteredList.addAll(endpointList);
        } else {
            String lowerQuery = query.toLowerCase().trim();
            for (Endpoint e : endpointList) {
                if (e.path.toLowerCase().contains(lowerQuery) || 
                    (e.description != null && e.description.toLowerCase().contains(lowerQuery)) ||
                    e.method.toLowerCase().contains(lowerQuery)) {
                    filteredList.add(e);
                }
            }
        }
        adapter.notifyDataSetChanged();
        updateEmptyState();
        updateCollectionMeta();
    }

    private void bindCollectionHeader() {
        tvProjectTitle.setText(collectionName);
        TextView headerTitle = findViewById(R.id.headerTitle);
        if (headerTitle != null) {
            headerTitle.setText(collectionName);
        }
        tvProjectSubtext.setText(!TextUtils.isEmpty(collectionDescription)
                ? collectionDescription
                : "Manage your collection APIs.");
        updateCollectionMeta();
    }

    private void updateEmptyState() {
        boolean isEmptyCollection = endpointList.isEmpty();
        if (emptyApiState != null) {
            emptyApiState.setVisibility(isEmptyCollection ? View.VISIBLE : View.GONE);
        }
        rvEndpoints.setVisibility(isEmptyCollection ? View.GONE : View.VISIBLE);
    }

    private void updateCollectionMeta() {
        if (tvCollectionMeta == null) return;
        String endpointLabel = endpointList.size() == 1 ? "1 API" : endpointList.size() + " APIs";
        String created = collectionCreatedAt > 0
                ? DateUtils.getRelativeTimeSpanString(collectionCreatedAt, System.currentTimeMillis(), DateUtils.MINUTE_IN_MILLIS).toString()
                : "recently";
        tvCollectionMeta.setText(collectionTemplate + " collection · " + endpointLabel + " · Created " + created);
    }

    private void testEntireCollection() {
        if (endpointList.isEmpty()) {
            Toast.makeText(this, "Add an API before testing this collection.", Toast.LENGTH_SHORT).show();
            return;
        }

        Intent intent = new Intent(this, CollectionTestResultActivity.class);
        intent.putExtra("PROJECT_ID", projectId);
        startActivity(intent);
    }

    private void showEditCollectionDialog() {
        LinearLayout container = new LinearLayout(this);
        container.setOrientation(LinearLayout.VERTICAL);
        int padding = (int) (20 * getResources().getDisplayMetrics().density);
        container.setPadding(padding, padding / 2, padding, 0);

        EditText etName = new EditText(this);
        etName.setHint("Collection name");
        etName.setSingleLine(true);
        etName.setText(collectionName);
        container.addView(etName);

        EditText etDescription = new EditText(this);
        etDescription.setHint("Description");
        etDescription.setMinLines(2);
        etDescription.setText(collectionDescription != null ? collectionDescription : "");
        container.addView(etDescription);

        Spinner spinnerTemplate = new Spinner(this);
        String[] templates = {"Empty", "REST API", "GraphQL"};
        ArrayAdapter<String> templateAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, templates);
        spinnerTemplate.setAdapter(templateAdapter);
        for (int i = 0; i < templates.length; i++) {
            if (templates[i].equalsIgnoreCase(collectionTemplate)) {
                spinnerTemplate.setSelection(i);
                break;
            }
        }
        container.addView(spinnerTemplate);

        AlertDialog dialog = new MaterialAlertDialogBuilder(this)
                .setTitle("Edit Collection")
                .setView(container)
                .setNegativeButton("Cancel", null)
                .setPositiveButton("Save", null)
                .create();

        dialog.setOnShowListener(d -> {
            Button save = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
            save.setOnClickListener(v -> {
                String name = etName.getText().toString().trim();
                String description = etDescription.getText().toString().trim();
                String template = spinnerTemplate.getSelectedItem().toString();

                if (TextUtils.isEmpty(name)) {
                    etName.setError("Collection name is required.");
                    return;
                }

                int updated = dbHelper.updateProject(projectId, name, description, template);
                if (updated > 0) {
                    collectionName = name;
                    collectionDescription = description;
                    collectionTemplate = template;
                    bindCollectionHeader();
                    Toast.makeText(this, "Collection updated.", Toast.LENGTH_SHORT).show();
                    dialog.dismiss();
                } else {
                    Toast.makeText(this, "Failed to update collection.", Toast.LENGTH_SHORT).show();
                }
            });
        });

        dialog.show();
    }

    // Endpoint Model Class
    public static class Endpoint {
        long id;
        String method;
        String path;
        String description;
        String url;
        String headers;
        String body;
        String authType;
        String authToken;
        String authUsername;
        String authPassword;

        public Endpoint(long id, String method, String path, String description, String url, String headers, String body,
                        String authType, String authToken, String authUsername, String authPassword) {
            this.id = id;
            this.method = method;
            this.path = path;
            this.description = description;
            this.url = url;
            this.headers = headers;
            this.body = body;
            this.authType = authType;
            this.authToken = authToken;
            this.authUsername = authUsername;
            this.authPassword = authPassword;
        }
    }

    public interface OnEndpointTestClickListener {
        void onTestClick(Endpoint endpoint);
    }

    public interface OnEndpointDeleteListener {
        void onDeleteClick(Endpoint endpoint);
    }

    // RecyclerView Adapter
    public static class EndpointsAdapter extends RecyclerView.Adapter<EndpointsAdapter.ViewHolder> {

        private final List<Endpoint> items;
        private final OnEndpointTestClickListener listener;
        private final OnEndpointDeleteListener deleteListener;

        public EndpointsAdapter(List<Endpoint> items, OnEndpointTestClickListener listener, OnEndpointDeleteListener deleteListener) {
            this.items = items;
            this.listener = listener;
            this.deleteListener = deleteListener;
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_endpoint_card, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            Endpoint item = items.get(position);
            holder.tvPath.setText(item.path);
            holder.tvDescription.setText(item.description != null && !item.description.isEmpty() 
                    ? item.description 
                    : "No description provided.");
            holder.tvMethodBadge.setText(item.method);

            // Notion Design System badge + accent stripe colors
            int accentColor = Color.parseColor("#d9f3e1"); // Mint – GET default
            int badgeBg = R.drawable.bg_badge_get;
            int badgeTextColor = Color.parseColor("#1aae39"); // Brand green

            switch (item.method.toUpperCase()) {
                case "POST":
                    accentColor = Color.parseColor("#e6e0f5"); // Lavender
                    badgeBg = R.drawable.bg_badge_post;
                    badgeTextColor = Color.parseColor("#391c57"); // Brand purple 800
                    break;
                case "PUT":
                    accentColor = Color.parseColor("#ffe8d4"); // Peach
                    badgeBg = R.drawable.bg_badge_put;
                    badgeTextColor = Color.parseColor("#793400"); // Brand orange deep
                    break;
                case "DELETE":
                    accentColor = Color.parseColor("#fde0ec"); // Rose
                    badgeBg = R.drawable.bg_badge_delete;
                    badgeTextColor = Color.parseColor("#e03131"); // Semantic error
                    break;
            }

            holder.vAccentStripe.setBackgroundColor(accentColor);
            holder.tvMethodBadge.setBackgroundResource(badgeBg);
            holder.tvMethodBadge.setTextColor(badgeTextColor);
            holder.tvPath.setTextColor(badgeTextColor);

            holder.btnTest.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onTestClick(item);
                }
            });

            holder.itemView.setOnLongClickListener(v -> {
                if (deleteListener != null) {
                    deleteListener.onDeleteClick(item);
                }
                return true;
            });
        }

        @Override
        public int getItemCount() {
            return items.size();
        }

        public static class ViewHolder extends RecyclerView.ViewHolder {
            TextView tvMethodBadge, tvVersion, tvPath, tvDescription;
            MaterialButton btnTest;
            View vAccentStripe;

            public ViewHolder(@NonNull View itemView) {
                super(itemView);
                tvMethodBadge = itemView.findViewById(R.id.tvMethodBadge);
                tvVersion = itemView.findViewById(R.id.tvVersion);
                tvPath = itemView.findViewById(R.id.tvPath);
                tvDescription = itemView.findViewById(R.id.tvDescription);
                btnTest = itemView.findViewById(R.id.btnTest);
                vAccentStripe = itemView.findViewById(R.id.vAccentStripe);
            }
        }
    }
}
