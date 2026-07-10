package com.bainian.memory.ui;

import android.app.Activity;
import android.app.Dialog;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.HorizontalScrollView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import com.bainian.memory.data.RecordRepository;
import com.bainian.memory.model.MemoryRecord;
import com.bainian.memory.util.UiKit;
import com.bainian.memory.view.SettingsIconView;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * 新增 / 编辑一条记录 —— 沉浸式全屏记录页。
 *
 * 设计取舍：记录一件事最自然的顺序是「先有画面或念头，再补充细节」，
 * 所以页面只保留两个核心元素——图片与正文，去掉标题/心情/标签等结构化字段，
 * 让「打开就能写、写完就能存」，把整理（标签）留到事后在时间线里按需补充。
 *
 * 需要宿主 Activity 通过 startActivityForResult 处理图片选择结果，
 * 并在 onActivityResult 中回调 {@link #handleImagePickResult}。
 */
public class RecordEditDialog {
    public static final int PICK_IMAGE_REQUEST = 701;

    private final Activity activity;
    private final RecordRepository repository;
    private final Runnable onSaved;

    private final List<Uri> pendingImageUris = new ArrayList<>();
    private LinearLayout imageStripRow;
    private View imageEmptyHint;

    public RecordEditDialog(Activity activity, RecordRepository repository, Runnable onSaved) {
        this.activity = activity;
        this.repository = repository;
        this.onSaved = onSaved;
    }

    public void show(LocalDate date, MemoryRecord editRecord) {
        pendingImageUris.clear();
        imageStripRow = null;

        boolean isEdit = editRecord != null;

        Dialog dialog = new Dialog(activity, android.R.style.Theme_Black_NoTitleBar_Fullscreen);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);

        LinearLayout root = new LinearLayout(activity);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(UiKit.getColorByName(activity, "bg"));

        // ═══ 顶部栏：取消 / 日期 / 保存 ═══
        LinearLayout topBar = new LinearLayout(activity);
        topBar.setOrientation(LinearLayout.HORIZONTAL);
        topBar.setGravity(Gravity.CENTER_VERTICAL);
        topBar.setPadding(dp(8), dp(14), dp(20), dp(10));
        root.setOnApplyWindowInsetsListener((v, insets) -> {
            topBar.setPadding(dp(8), insets.getSystemWindowInsetTop() + dp(14), dp(20), dp(10));
            return insets;
        });

        TextView cancelBtn = new TextView(activity);
        cancelBtn.setText("取消");
        cancelBtn.setTextSize(15);
        cancelBtn.setTextColor(UiKit.getColorByName(activity, "muted"));
        cancelBtn.setPadding(dp(12), dp(8), dp(12), dp(8));
        cancelBtn.setOnClickListener(v -> dialog.dismiss());
        topBar.addView(cancelBtn);

        TextView dateLabel = new TextView(activity);
        dateLabel.setText(date.format(DateTimeFormatter.ofPattern("M月d日 · EEEE", Locale.CHINA)));
        dateLabel.setTextSize(15);
        dateLabel.setTextColor(UiKit.getColorByName(activity, "ink_soft"));
        dateLabel.setGravity(Gravity.CENTER);
        topBar.addView(dateLabel, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

        TextView saveBtn = new TextView(activity);
        saveBtn.setText("保存");
        saveBtn.setTextSize(15);
        saveBtn.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
        saveBtn.setTextColor(UiKit.getColorByName(activity, "accent"));
        saveBtn.setPadding(dp(12), dp(8), dp(12), dp(8));
        topBar.addView(saveBtn);

        root.addView(topBar);

        // ═══ 可滚动内容区：图片 + 正文 ═══
        ScrollView scrollView = new ScrollView(activity);
        scrollView.setFillViewport(true);
        LinearLayout content = new LinearLayout(activity);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(dp(20), dp(4), dp(20), dp(20));
        scrollView.addView(content, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        // —— 图片区：空状态为大引导区，有图后变为横向缩略图条 ——
        FrameLayout imageArea = new FrameLayout(activity);
        imageArea.setLayoutParams(WidgetLp.fullWidth(dp(0), dp(16)));
        content.addView(imageArea);

        imageEmptyHint = buildImageEmptyHint();
        imageArea.addView(imageEmptyHint);

        HorizontalScrollView imageScroll = new HorizontalScrollView(activity);
        imageScroll.setHorizontalScrollBarEnabled(false);
        imageScroll.setVisibility(View.GONE);
        imageStripRow = new LinearLayout(activity);
        imageStripRow.setOrientation(LinearLayout.HORIZONTAL);
        imageStripRow.setGravity(Gravity.CENTER_VERTICAL);
        imageScroll.addView(imageStripRow);
        imageArea.addView(imageScroll, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(96)));
        final HorizontalScrollView imageScrollRef = imageScroll;

        // —— 正文：唯一的核心输入框，占据主要空间 ——
        EditText body = new EditText(activity);
        body.setHint("这一刻，发生了什么？");
        body.setTextSize(17);
        body.setLineSpacing(dp(6), 1f);
        body.setTextColor(UiKit.getColorByName(activity, "ink"));
        body.setHintTextColor(UiKit.getColorByName(activity, "muted_light"));
        body.setBackground(null);
        body.setGravity(Gravity.TOP | Gravity.START);
        body.setMinLines(6);
        body.setInputType(android.text.InputType.TYPE_CLASS_TEXT
                | android.text.InputType.TYPE_TEXT_FLAG_MULTI_LINE
                | android.text.InputType.TYPE_TEXT_FLAG_CAP_SENTENCES);
        content.addView(body, WidgetLp.fullWidth(0, dp(4)));

        root.addView(scrollView, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));

        // ═══ 底部：添加照片入口（常驻，随时可继续加图） ═══
        LinearLayout bottomBar = new LinearLayout(activity);
        bottomBar.setOrientation(LinearLayout.HORIZONTAL);
        bottomBar.setGravity(Gravity.CENTER_VERTICAL);
        bottomBar.setPadding(dp(12), dp(10), dp(12), dp(14));
        View bottomDivider = new View(activity);
        bottomDivider.setBackgroundColor(UiKit.getColorByName(activity, "line"));
        root.addView(bottomDivider, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(1)));

        LinearLayout photoEntry = new LinearLayout(activity);
        photoEntry.setOrientation(LinearLayout.HORIZONTAL);
        photoEntry.setGravity(Gravity.CENTER_VERTICAL);
        photoEntry.setPadding(dp(10), dp(8), dp(10), dp(8));
        photoEntry.setOnClickListener(v -> pickImage());
        SettingsIconView photoIcon = new SettingsIconView(activity);
        photoIcon.setKind(SettingsIconView.Kind.CAMERA);
        photoIcon.setIconColor(UiKit.getColorByName(activity, "ink_soft"));
        int photoIconSize = dp(16);
        LinearLayout.LayoutParams photoIconLp = new LinearLayout.LayoutParams(photoIconSize, photoIconSize);
        photoIconLp.setMarginEnd(dp(6));
        photoEntry.addView(photoIcon, photoIconLp);
        TextView photoLabel = new TextView(activity);
        photoLabel.setText("照片");
        photoLabel.setTextSize(14);
        photoLabel.setTextColor(UiKit.getColorByName(activity, "ink_soft"));
        photoEntry.addView(photoLabel);
        bottomBar.addView(photoEntry);

        root.addView(bottomBar);

        // 编辑模式：预填充已有内容
        if (isEdit) {
            body.setText(editRecord.body.isEmpty() ? editRecord.title : editRecord.body);
            for (String s : editRecord.getImageUris()) {
                try { pendingImageUris.add(Uri.parse(s)); } catch (Exception ignored) {}
            }
        }
        refreshImageStrip(imageScrollRef);
        body.requestFocus();

        final MemoryRecord target = editRecord;
        saveBtn.setOnClickListener(v -> {
            String bt = body.getText().toString().trim();
            if (bt.isEmpty() && pendingImageUris.isEmpty()) {
                Toast.makeText(activity, "写点什么，或加一张照片吧", Toast.LENGTH_SHORT).show();
                return;
            }
            List<String> uris = new ArrayList<>();
            for (Uri u : pendingImageUris) uris.add(u.toString());
            String autoTitle = extractTitle(bt);

            if (isEdit && target != null) {
                target.title = autoTitle;
                target.body = bt;
                target.setImageUris(uris);
                repository.update();
            } else {
                MemoryRecord rec = new MemoryRecord();
                rec.id = String.valueOf(System.currentTimeMillis());
                rec.date = date.format(RecordRepository.DATE_KEY);
                rec.title = autoTitle;
                rec.body = bt;
                rec.setImageUris(uris);
                repository.add(rec);
            }
            onSaved.run();
            dialog.dismiss();
        });

        dialog.setContentView(root);
        Window window = dialog.getWindow();
        if (window != null) {
            window.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            window.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
        }
        dialog.show();
    }

    /** 取正文首句（按标点或换行截断，最长 20 字）作为列表展示用的标题 */
    private String extractTitle(String body) {
        if (body.isEmpty()) return "未命名的一天";
        String firstLine = body.split("\\r?\\n", 2)[0];
        String[] parts = firstLine.split("[。！？.!?]", 2);
        String first = parts[0].trim();
        if (first.isEmpty()) first = firstLine.trim();
        if (first.length() > 20) first = first.substring(0, 20) + "…";
        return first.isEmpty() ? "未命名的一天" : first;
    }

    private View buildImageEmptyHint() {
        LinearLayout hint = new LinearLayout(activity);
        hint.setOrientation(LinearLayout.VERTICAL);
        hint.setGravity(Gravity.CENTER);
        hint.setBackground(UiKit.getDrawableByName(activity, "input_bg"));
        hint.setPadding(dp(16), dp(20), dp(16), dp(20));
        hint.setOnClickListener(v -> pickImage());

        TextView icon = new TextView(activity);
        icon.setText("＋");
        icon.setTextSize(22);
        icon.setTextColor(UiKit.getColorByName(activity, "accent"));
        icon.setGravity(Gravity.CENTER);
        hint.addView(icon);

        TextView label = new TextView(activity);
        label.setText("添加这一刻的照片");
        label.setTextSize(13);
        label.setTextColor(UiKit.getColorByName(activity, "muted"));
        label.setGravity(Gravity.CENTER);
        label.setPadding(0, dp(4), 0, 0);
        hint.addView(label);

        hint.setLayoutParams(new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(96)));
        return hint;
    }

    private void refreshImageStrip(HorizontalScrollView imageScroll) {
        if (imageStripRow == null) return;
        imageStripRow.removeAllViews();

        boolean hasImages = !pendingImageUris.isEmpty();
        imageEmptyHint.setVisibility(hasImages ? View.GONE : View.VISIBLE);
        imageScroll.setVisibility(hasImages ? View.VISIBLE : View.GONE);
        if (!hasImages) return;

        for (int i = 0; i < pendingImageUris.size(); i++) {
            final int idx = i;
            Uri uri = pendingImageUris.get(i);
            FrameLayout wrap = new FrameLayout(activity);
            LinearLayout.LayoutParams wp = new LinearLayout.LayoutParams(dp(96), dp(96));
            wp.rightMargin = dp(10);
            wrap.setLayoutParams(wp);

            ImageView thumb = new ImageView(activity);
            thumb.setScaleType(ImageView.ScaleType.CENTER_CROP);
            thumb.setImageURI(uri);
            thumb.setClipToOutline(true);
            thumb.setOutlineProvider(new android.view.ViewOutlineProvider() {
                @Override
                public void getOutline(View view, android.graphics.Outline outline) {
                    outline.setRoundRect(0, 0, view.getWidth(), view.getHeight(), dp(12));
                }
            });
            thumb.setLayoutParams(new FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
            wrap.addView(thumb);

            // 删除按钮——矢量叉号替代文本符号 ✕，与全局图标语言保持一致
            FrameLayout x = new FrameLayout(activity);
            x.setBackgroundColor(0x99000000);
            FrameLayout.LayoutParams xp = new FrameLayout.LayoutParams(dp(20), dp(20));
            xp.gravity = Gravity.TOP | Gravity.END;
            xp.topMargin = dp(4);
            xp.rightMargin = dp(4);
            x.setOnClickListener(v -> {
                pendingImageUris.remove(idx);
                refreshImageStrip(imageScroll);
            });
            SettingsIconView xIcon = new SettingsIconView(activity);
            xIcon.setKind(SettingsIconView.Kind.CLOSE);
            xIcon.setIconColor(Color.WHITE);
            int xIconSize = dp(10);
            FrameLayout.LayoutParams xIconLp = new FrameLayout.LayoutParams(xIconSize, xIconSize);
            xIconLp.gravity = Gravity.CENTER;
            x.addView(xIcon, xIconLp);
            wrap.addView(x, xp);

            imageStripRow.addView(wrap);
        }

        // 末尾追加一个「继续添加」按钮
        TextView addMore = new TextView(activity);
        addMore.setText("＋");
        addMore.setTextSize(20);
        addMore.setTextColor(UiKit.getColorByName(activity, "accent"));
        addMore.setGravity(Gravity.CENTER);
        addMore.setBackground(UiKit.getDrawableByName(activity, "input_bg"));
        addMore.setLayoutParams(new LinearLayout.LayoutParams(dp(96), dp(96)));
        addMore.setOnClickListener(v -> pickImage());
        imageStripRow.addView(addMore);
    }

    private void pickImage() {
        Intent i = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        i.addCategory(Intent.CATEGORY_OPENABLE);
        i.setType("image/*");
        i.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
        i.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION);
        activity.startActivityForResult(i, PICK_IMAGE_REQUEST);
    }

    /** 由宿主 Activity 在 onActivityResult 中回调，处理图片选择结果 */
    public void handleImagePickResult(int resultCode, Intent data) {
        if (resultCode != Activity.RESULT_OK || data == null) return;
        int added = 0;
        if (data.getClipData() != null) {
            for (int i = 0; i < data.getClipData().getItemCount(); i++) {
                Uri uri = data.getClipData().getItemAt(i).getUri();
                if (uri != null) { grantPersist(uri); pendingImageUris.add(uri); added++; }
            }
        } else if (data.getData() != null) {
            Uri uri = data.getData();
            grantPersist(uri);
            pendingImageUris.add(uri);
            added++;
        }
        if (added > 0 && imageStripRow != null) {
            HorizontalScrollView parent = (HorizontalScrollView) imageStripRow.getParent();
            refreshImageStrip(parent);
        }
    }

    private void grantPersist(Uri uri) {
        try {
            activity.getContentResolver().takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION);
        } catch (SecurityException ignored) {}
    }

    private int dp(int v) {
        return UiKit.dp(activity, v);
    }

    /** 局部小工具：生成撑满宽度、带上下 margin 的 LinearLayout.LayoutParams */
    private static final class WidgetLp {
        static LinearLayout.LayoutParams fullWidth(int top, int bottom) {
            LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            p.setMargins(0, top, 0, bottom);
            return p;
        }
    }
}
