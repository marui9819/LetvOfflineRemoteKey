package com.marui.letvremoteoffline;

import android.app.Activity;
import android.app.AlertDialog;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.os.IBinder;
import android.os.Parcel;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.InputStream;
import java.io.ByteArrayOutputStream;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

public final class MainActivity extends Activity {
    private static final String SERVICE_NAME = "android.letvremoteupgrademanager";
    private static final int MODIFY_KEY_TRANSACTION = 12;

    // Exact local LeTV TV pulse data recovered from the SHC Air system app.
    private static final int[] LETV_POWER_PULSES = {
            8931, 4479, 507, 585, 507, 585, 507, 1719, 507, 1719,
            507, 585, 507, 585, 507, 1719, 507, 598, 507, 1719,
            507, 585, 507, 1719, 507, 585, 507, 585, 507, 1719,
            507, 1719, 507, 598, 507, 585, 507, 1719, 507, 585,
            507, 1719, 507, 585, 507, 585, 507, 585, 507, 598,
            507, 1719, 507, 585, 507, 1719, 507, 585, 506, 1719,
            507, 1719, 507, 1719, 507, 1724, 537, 42078, 8910, 2273,
            532, 64468, 0, 31795
    };

    private final ExecutorService worker = Executors.newSingleThreadExecutor();
    private final AtomicBoolean busy = new AtomicBoolean(false);
    private final List<CodeProfile> profiles = new ArrayList<>();
    private TextView statusView;
    private Button checkButton;
    private Button brandButton;
    private Button modelButton;
    private Button writeButton;
    private TextView profileInfoView;
    private CodeProfile selectedProfile;
    private volatile boolean serviceAvailable;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
        setContentView(createContent());
        loadProfiles();
        refreshProfileControls();
        checkService(false);
    }

    private View createContent() {
        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        scroll.setBackgroundColor(Color.rgb(7, 17, 31));

        LinearLayout page = new LinearLayout(this);
        page.setOrientation(LinearLayout.VERTICAL);
        page.setGravity(Gravity.CENTER_VERTICAL);
        page.setPadding(dp(72), dp(38), dp(72), dp(34));
        scroll.addView(page, new ScrollView.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

        TextView title = text("屏霸 TV 键离线配对", 34, Color.WHITE, true);
        page.addView(title, matchWrap());

        TextView subtitle = text("离线选择多个品牌电视的红外电源码组", 20,
                Color.rgb(177, 197, 219), false);
        LinearLayout.LayoutParams subtitleParams = matchWrap();
        subtitleParams.topMargin = dp(8);
        page.addView(subtitle, subtitleParams);

        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(dp(24), dp(18), dp(24), dp(18));
        card.setBackground(roundRect(Color.rgb(16, 32, 52), dp(14), Color.rgb(45, 74, 104), dp(1)));
        LinearLayout.LayoutParams cardParams = matchWrap();
        cardParams.topMargin = dp(22);
        page.addView(card, cardParams);

        TextView mode = text("离线配置  ·  不请求云端  ·  不需要电脑服务", 19,
                Color.rgb(66, 215, 255), true);
        card.addView(mode, matchWrap());
        statusView = text("正在检查屏霸遥控器服务…", 19, Color.WHITE, false);
        LinearLayout.LayoutParams statusParams = matchWrap();
        statusParams.topMargin = dp(12);
        card.addView(statusView, statusParams);

        brandButton = makeButton("选择电视品牌", false);
        modelButton = makeButton("选择遥控器码组", false);
        profileInfoView = text("码组信息加载中…", 16, Color.rgb(177, 197, 219), false);
        LinearLayout.LayoutParams selectorParams = matchWrap();
        selectorParams.topMargin = dp(12);
        card.addView(brandButton, selectorParams);
        LinearLayout.LayoutParams modelParams = matchWrap();
        modelParams.topMargin = dp(8);
        card.addView(modelButton, modelParams);
        LinearLayout.LayoutParams codeParams = matchWrap();
        codeParams.topMargin = dp(8);
        card.addView(profileInfoView, codeParams);

        checkButton = makeButton("重新检查连接", false);
        writeButton = makeButton("写入所选电视电源码", true);
        LinearLayout.LayoutParams buttonParams = matchWrap();
        buttonParams.topMargin = dp(17);
        page.addView(checkButton, buttonParams);
        LinearLayout.LayoutParams writeParams = matchWrap();
        writeParams.topMargin = dp(10);
        page.addView(writeButton, writeParams);
        writeButton.setEnabled(false);

        TextView note = text("写入后请将遥控器对准电视，手动按 TV 键测试。品牌相同也可能因电视/遥控器型号不同而不兼容；可逐个尝试该品牌的候选码组。", 17,
                Color.rgb(177, 197, 219), false);
        LinearLayout.LayoutParams noteParams = matchWrap();
        noteParams.topMargin = dp(14);
        page.addView(note, noteParams);

        TextView warning = text("注意：当前 TV 键码无法回读备份。写入会覆盖现有码；你当前验证成功的乐视码也可重新选择后恢复。写入过程中请勿断电或拔下接收器。", 15,
                Color.rgb(255, 196, 119), false);
        LinearLayout.LayoutParams warningParams = matchWrap();
        warningParams.topMargin = dp(12);
        page.addView(warning, warningParams);

        checkButton.setOnClickListener(v -> checkService(true));
        brandButton.setOnClickListener(v -> showBrandPicker());
        modelButton.setOnClickListener(v -> showModelPicker());
        writeButton.setOnClickListener(v -> confirmWrite());
        page.setFocusableInTouchMode(true);
        page.post(() -> checkButton.requestFocus());
        return scroll;
    }

    private Button makeButton(String label, boolean primary) {
        Button button = new Button(this);
        button.setText(label);
        button.setAllCaps(false);
        button.setTextSize(20);
        button.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
        button.setTextColor(primary ? Color.rgb(5, 24, 40) : Color.WHITE);
        button.setMinHeight(dp(58));
        button.setPadding(dp(24), dp(10), dp(24), dp(10));
        button.setFocusable(true);
        button.setFocusableInTouchMode(true);
        button.setBackground(roundRect(primary ? Color.rgb(48, 204, 244) : Color.rgb(28, 52, 78),
                dp(12), Color.rgb(90, 174, 218), dp(2)));
        button.setOnFocusChangeListener((v, focused) -> {
            button.setScaleX(focused ? 1.025f : 1f);
            button.setScaleY(focused ? 1.025f : 1f);
            button.setBackground(roundRect(
                    primary
                            ? (focused ? Color.rgb(112, 231, 255) : Color.rgb(48, 204, 244))
                            : (focused ? Color.rgb(41, 82, 119) : Color.rgb(28, 52, 78)),
                    dp(12), focused ? Color.WHITE : Color.rgb(90, 174, 218), focused ? dp(3) : dp(2)));
        });
        return button;
    }

    private void checkService(boolean showChecking) {
        if (!busy.compareAndSet(false, true)) return;
        setControlsEnabled(false);
        if (showChecking) setStatus("正在只读检查遥控器服务…", Color.WHITE);
        worker.execute(() -> {
            ServiceHandle handle = findService();
            runOnUiThread(() -> {
                busy.set(false);
                serviceAvailable = handle.binder != null;
                setControlsEnabled(true);
                if (serviceAvailable) {
                    setStatus("已连接屏霸服务\n接口：" + handle.descriptor, Color.rgb(116, 235, 171));
                } else {
                    setStatus("未找到屏霸遥控器服务\n" + handle.error, Color.rgb(255, 153, 141));
                }
                writeButton.setEnabled(serviceAvailable && selectedProfile != null);
            });
        });
    }

    private ServiceHandle findService() {
        try {
            Class<?> serviceManager = Class.forName("android.os.ServiceManager");
            Method getService = serviceManager.getDeclaredMethod("getService", String.class);
            getService.setAccessible(true);
            IBinder binder = (IBinder) getService.invoke(null, SERVICE_NAME);
            if (binder == null) return new ServiceHandle(null, "", "系统没有返回服务句柄");
            String descriptor = binder.getInterfaceDescriptor();
            if (descriptor == null || descriptor.trim().isEmpty()) {
                return new ServiceHandle(null, "", "服务没有提供接口描述");
            }
            return new ServiceHandle(binder, descriptor, "");
        } catch (Throwable error) {
            return new ServiceHandle(null, "", describe(error));
        }
    }

    private void confirmWrite() {
        if (!serviceAvailable || busy.get() || selectedProfile == null) return;
        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle("确认写入遥控器")
                .setMessage("即将写入：" + selectedProfile.brand + " / " + selectedProfile.model
                        + "\n载波频率：" + selectedProfile.frequency + " Hz"
                        + "\n\n遥控器现有 TV 键码无法回读备份，此操作会覆盖它。写入后请手动按 TV 键测试；若无效，可回来选择该品牌的其他候选码组。是否继续？")
                .setNegativeButton("取消", (dlg, which) -> dlg.dismiss())
                .setPositiveButton("确认写入", null)
                .create();
        dialog.setOnShowListener(ignored -> dialog.getButton(AlertDialog.BUTTON_POSITIVE)
                .setOnClickListener(v -> {
                    dialog.dismiss();
                    startWrite();
                }));
        dialog.show();
    }

    private void startWrite() {
        final CodeProfile profile = selectedProfile;
        if (profile == null) return;
        if (!busy.compareAndSet(false, true)) return;
        setControlsEnabled(false);
        setStatus("正在写入遥控器，请勿断电或拔出接收器…", Color.rgb(255, 220, 133));
        worker.execute(() -> {
            String result;
            int code = Integer.MIN_VALUE;
            try {
                ServiceHandle handle = findService();
                if (handle.binder == null) throw new IllegalStateException(handle.error);
                Parcel data = Parcel.obtain();
                Parcel reply = Parcel.obtain();
                try {
                    data.writeInterfaceToken(handle.descriptor);
                    data.writeInt(profile.frequency);
                    data.writeInt(profile.pulses.length);
                    for (int pulse : profile.pulses) data.writeInt(pulse);
                    boolean handled = handle.binder.transact(MODIFY_KEY_TRANSACTION, data, reply, 0);
                    if (!handled) throw new IllegalStateException("遥控器服务没有处理写入请求");
                    code = reply.readInt();
                    if (code == 0) {
                        result = "服务确认已写入（ACK 状态 0）。这只确认码组已发送给屏霸，不代表电视已兼容；请对准电视手动按 TV 键测试。";
                    } else {
                        result = "服务返回写入失败（状态 " + code + "）。电视键码可能未更新。";
                    }
                } finally {
                    data.recycle();
                    reply.recycle();
                }
            } catch (Throwable error) {
                result = "写入未能完成：" + describe(error);
            }
            final String message = result;
            final boolean success = code == 0;
            runOnUiThread(() -> {
                busy.set(false);
                serviceAvailable = findService().binder != null;
                setControlsEnabled(true);
                writeButton.setEnabled(serviceAvailable && selectedProfile != null);
                setStatus(message, success ? Color.rgb(116, 235, 171) : Color.rgb(255, 153, 141));
            });
        });
    }

    private void setControlsEnabled(boolean enabled) {
        checkButton.setEnabled(enabled);
        brandButton.setEnabled(enabled);
        modelButton.setEnabled(enabled && selectedProfile != null);
        writeButton.setEnabled(enabled && serviceAvailable && selectedProfile != null);
    }

    private void loadProfiles() {
        profiles.clear();
        profiles.add(new CodeProfile("乐视", "SHC Air 内置验证码（恢复用）", 37810,
                LETV_POWER_PULSES, "屏霸系统内置码；用户已实测可用"));
        try (InputStream input = getAssets().open("ir_profiles.json")) {
            ByteArrayOutputStream output = new ByteArrayOutputStream();
            byte[] buffer = new byte[4096];
            int count;
            while ((count = input.read(buffer)) >= 0) {
                output.write(buffer, 0, count);
            }
            JSONObject root = new JSONObject(new String(output.toByteArray(), StandardCharsets.UTF_8));
            JSONArray entries = root.getJSONArray("profiles");
            for (int i = 0; i < entries.length(); i++) {
                JSONObject entry = entries.getJSONObject(i);
                JSONArray pulseArray = entry.getJSONArray("pulses");
                int[] pulses = new int[pulseArray.length()];
                for (int p = 0; p < pulseArray.length(); p++) pulses[p] = pulseArray.getInt(p);
                profiles.add(new CodeProfile(entry.getString("brand"), entry.getString("model"),
                        entry.getInt("frequency"), pulses, entry.optString("source", "离线码库")));
            }
        } catch (Exception error) {
            setStatus("离线品牌码库读取失败：" + describe(error), Color.rgb(255, 153, 141));
        }
        if (!profiles.isEmpty()) selectedProfile = profiles.get(0);
    }

    private void showBrandPicker() {
        Set<String> unique = new LinkedHashSet<>();
        for (CodeProfile profile : profiles) unique.add(profile.brand);
        List<String> brands = new ArrayList<>(unique);
        Collections.sort(brands, (left, right) -> {
            if (left.equals("乐视")) return -1;
            if (right.equals("乐视")) return 1;
            return left.compareTo(right);
        });
        int selectedIndex = Math.max(0, brands.indexOf(selectedProfile == null ? "" : selectedProfile.brand));
        List<String> labels = new ArrayList<>();
        for (String brand : brands) labels.add(brand);
        showChoiceDialog("选择电视品牌", labels, selectedIndex, which -> {
            String brand = brands.get(which);
            for (CodeProfile profile : profiles) {
                if (profile.brand.equals(brand)) {
                    selectedProfile = profile;
                    break;
                }
            }
            refreshProfileControls();
        });
    }

    private void showModelPicker() {
        if (selectedProfile == null) return;
        List<CodeProfile> choices = new ArrayList<>();
        for (CodeProfile profile : profiles) {
            if (profile.brand.equals(selectedProfile.brand)) choices.add(profile);
        }
        List<String> items = new ArrayList<>();
        int selectedIndex = 0;
        for (int i = 0; i < choices.size(); i++) {
            CodeProfile profile = choices.get(i);
            items.add(profile.model + "  ·  " + profile.frequency + " Hz");
            if (profile == selectedProfile) selectedIndex = i;
        }
        showChoiceDialog("选择候选遥控器码组", items, selectedIndex, which -> {
            selectedProfile = choices.get(which);
            refreshProfileControls();
        });
    }

    private void showChoiceDialog(String title, List<String> choices, int selectedIndex, ChoiceAction action) {
        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(false);
        scroll.setBackgroundColor(Color.rgb(7, 17, 31));

        LinearLayout options = new LinearLayout(this);
        options.setOrientation(LinearLayout.VERTICAL);
        options.setPadding(dp(12), dp(8), dp(12), dp(8));
        scroll.addView(options, new ScrollView.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        final AlertDialog[] holder = new AlertDialog[1];
        List<Button> buttons = new ArrayList<>();
        for (int i = 0; i < choices.size(); i++) {
            final int index = i;
            boolean selected = i == selectedIndex;
            String marker = selected ? "✓  当前选择  ·  " : "选择  ·  ";
            Button option = makeChoiceButton(marker + choices.get(i), selected);
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            params.bottomMargin = dp(7);
            options.addView(option, params);
            buttons.add(option);
            option.setOnClickListener(v -> {
                if (holder[0] != null) holder[0].dismiss();
                action.onChoice(index);
            });
            option.setOnKeyListener((view, keyCode, event) -> {
                if (event.getAction() != KeyEvent.ACTION_DOWN) return false;
                int target = -1;
                if (keyCode == KeyEvent.KEYCODE_DPAD_DOWN) target = Math.min(buttons.size() - 1, index + 1);
                else if (keyCode == KeyEvent.KEYCODE_DPAD_UP) target = Math.max(0, index - 1);
                else if (keyCode == KeyEvent.KEYCODE_DPAD_CENTER
                        || keyCode == KeyEvent.KEYCODE_ENTER
                        || keyCode == KeyEvent.KEYCODE_NUMPAD_ENTER) {
                    option.performClick();
                    return true;
                }
                if (target >= 0) {
                    Button next = buttons.get(target);
                    next.requestFocus();
                    scroll.post(() -> {
                        int top = next.getTop();
                        int bottom = top + next.getHeight();
                        int scrollTop = scroll.getScrollY();
                        int scrollBottom = scrollTop + scroll.getHeight();
                        if (top < scrollTop) scroll.smoothScrollTo(0, top);
                        else if (bottom > scrollBottom) scroll.smoothScrollTo(0, bottom - scroll.getHeight());
                    });
                    return true;
                }
                return keyCode == KeyEvent.KEYCODE_DPAD_UP || keyCode == KeyEvent.KEYCODE_DPAD_DOWN;
            });
        }

        int listHeight = Math.min(choices.size() * dp(68) + dp(16), dp(540));
        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle(title + "  ·  方向键移动，确定选择")
                .setView(scroll)
                .setNegativeButton("取消", null)
                .create();
        holder[0] = dialog;
        dialog.setOnShowListener(ignored -> {
            scroll.getLayoutParams().height = listHeight;
            scroll.requestLayout();
            if (selectedIndex >= 0 && selectedIndex < buttons.size()) {
                buttons.get(selectedIndex).requestFocus();
            } else if (!buttons.isEmpty()) {
                buttons.get(0).requestFocus();
            }
        });
        dialog.show();
    }

    private Button makeChoiceButton(String label, boolean selected) {
        Button button = new Button(this);
        button.setText(label);
        button.setAllCaps(false);
        button.setGravity(Gravity.CENTER_VERTICAL | Gravity.START);
        button.setTextSize(19);
        button.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
        button.setTextColor(Color.WHITE);
        button.setMinHeight(dp(58));
        button.setPadding(dp(20), dp(9), dp(20), dp(9));
        button.setFocusable(true);
        button.setFocusableInTouchMode(true);
        button.setBackground(choiceBackground(selected, false));
        button.setOnFocusChangeListener((view, focused) -> {
            button.setScaleX(focused ? 1.02f : 1f);
            button.setScaleY(focused ? 1.02f : 1f);
            button.setTextColor(Color.WHITE);
            button.setBackground(choiceBackground(selected, focused));
        });
        return button;
    }

    private GradientDrawable choiceBackground(boolean selected, boolean focused) {
        int fill;
        int stroke;
        int strokeWidth;
        if (focused) {
            fill = Color.rgb(0, 117, 190);
            stroke = Color.WHITE;
            strokeWidth = dp(4);
        } else if (selected) {
            fill = Color.rgb(15, 67, 91);
            stroke = Color.rgb(42, 220, 255);
            strokeWidth = dp(3);
        } else {
            fill = Color.rgb(24, 42, 62);
            stroke = Color.rgb(93, 122, 151);
            strokeWidth = dp(1);
        }
        return roundRect(fill, dp(10), stroke, strokeWidth);
    }

    private void refreshProfileControls() {
        if (brandButton == null || modelButton == null || profileInfoView == null) return;
        if (selectedProfile == null) {
            brandButton.setText("选择电视品牌");
            modelButton.setText("没有可用码组");
            modelButton.setEnabled(false);
            profileInfoView.setText("没有找到可用的离线电源码组。");
            writeButton.setEnabled(false);
            return;
        }
        brandButton.setText("电视品牌：" + selectedProfile.brand + "（点击更换）");
        int modelCount = 0;
        for (CodeProfile profile : profiles) {
            if (profile.brand.equals(selectedProfile.brand)) modelCount++;
        }
        modelButton.setText("码组：" + selectedProfile.model + "（" + modelCount + " 个候选，点击选择）");
        modelButton.setEnabled(true);
        String sourceLabel = selectedProfile.source;
        int lastSlash = sourceLabel.lastIndexOf('/');
        if (lastSlash >= 0 && lastSlash + 1 < sourceLabel.length()) {
            sourceLabel = "Flipper-IRDB · " + sourceLabel.substring(lastSlash + 1);
        }
        profileInfoView.setText("载波 " + selectedProfile.frequency + " Hz  ·  "
                + selectedProfile.pulses.length + " 个时序值\n来源：" + sourceLabel);
        writeButton.setEnabled(serviceAvailable && !busy.get());
    }

    private void setStatus(String value, int color) {
        statusView.setText(value);
        statusView.setTextColor(color);
    }

    private static String describe(Throwable error) {
        Throwable cause = error;
        while (cause.getCause() != null) cause = cause.getCause();
        String text = cause.getMessage();
        return cause.getClass().getSimpleName() + (text == null || text.isEmpty() ? "" : ": " + text);
    }

    private TextView text(String value, int sizeSp, int color, boolean bold) {
        TextView view = new TextView(this);
        view.setText(value);
        view.setTextSize(sizeSp);
        view.setTextColor(color);
        view.setGravity(Gravity.CENTER_VERTICAL);
        view.setLineSpacing(dp(4), 1f);
        view.setTypeface(Typeface.create(bold ? "sans-serif-medium" : "sans-serif", Typeface.NORMAL));
        return view;
    }

    private GradientDrawable roundRect(int fill, int radius, int stroke, int strokeWidth) {
        GradientDrawable drawable = new GradientDrawable();
        drawable.setColor(fill);
        drawable.setCornerRadius(radius);
        drawable.setStroke(strokeWidth, stroke);
        return drawable;
    }

    private LinearLayout.LayoutParams matchWrap() {
        return new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT);
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }

    @Override
    protected void onDestroy() {
        worker.shutdown();
        super.onDestroy();
    }

    private static final class ServiceHandle {
        final IBinder binder;
        final String descriptor;
        final String error;

        ServiceHandle(IBinder binder, String descriptor, String error) {
            this.binder = binder;
            this.descriptor = descriptor;
            this.error = error;
        }
    }

    private static final class CodeProfile {
        final String brand;
        final String model;
        final int frequency;
        final int[] pulses;
        final String source;

        CodeProfile(String brand, String model, int frequency, int[] pulses, String source) {
            this.brand = brand;
            this.model = model;
            this.frequency = frequency;
            this.pulses = pulses;
            this.source = source;
        }
    }

    private interface ChoiceAction {
        void onChoice(int index);
    }
}
