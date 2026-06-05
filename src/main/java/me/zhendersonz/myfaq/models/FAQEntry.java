package me.zhendersonz.myfaq.models;

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

public class FAQEntry {

    private final String id;
    private final List<String> keywords;
    private final String regex;
    private final List<String> responses;
    private final String command;
    private final String responseType;
    private final String permission;
    private final String permissionDenied;
    private final String clickableText;
    private final String clickableCommand;
    private final String clickableUrl;
    private final String sound;
    private final long eventStart;
    private final long eventEnd;
    private int metricsCount;

    public FAQEntry(String id, List<String> keywords, String regex, List<String> responses,
                    String command, String responseType, String permission,
                    String permissionDenied, String clickableText,
                    String clickableCommand, String clickableUrl, String sound,
                    long eventStart, long eventEnd) {
        this.id = id;
        this.keywords = keywords;
        this.regex = regex;
        this.responses = responses;
        this.command = command;
        this.responseType = responseType;
        this.permission = permission;
        this.permissionDenied = permissionDenied;
        this.clickableText = clickableText;
        this.clickableCommand = clickableCommand;
        this.clickableUrl = clickableUrl;
        this.sound = sound;
        this.eventStart = eventStart;
        this.eventEnd = eventEnd;
        this.metricsCount = 0;
    }

    public String getId() { return id; }
    public List<String> getKeywords() { return keywords; }
    public String getRegex() { return regex; }
    public List<String> getResponses() { return responses; }
    public String getCommand() { return command; }
    public String getResponseType() { return responseType; }
    public String getPermission() { return permission; }
    public String getPermissionDenied() { return permissionDenied; }
    public String getClickableText() { return clickableText; }
    public String getClickableCommand() { return clickableCommand; }
    public String getClickableUrl() { return clickableUrl; }
    public String getSound() { return sound; }
    public long getEventStart() { return eventStart; }
    public long getEventEnd() { return eventEnd; }
    public int getMetricsCount() { return metricsCount; }
    public void setMetricsCount(int count) { this.metricsCount = count; }
    public void incrementMetrics() { this.metricsCount++; }

    public String getRandomResponse() {
        if (responses.isEmpty()) return "";
        return responses.get(ThreadLocalRandom.current().nextInt(responses.size()));
    }

    public boolean isEventActive() {
        long now = System.currentTimeMillis();
        if (eventStart == 0 && eventEnd == 0) return true;
        if (eventStart > 0 && now < eventStart) return false;
        if (eventEnd > 0 && now > eventEnd) return false;
        return true;
    }

    public boolean hasRegex() {
        return regex != null && !regex.isEmpty();
    }

    public boolean hasCommand() {
        return command != null && !command.isEmpty();
    }

    public boolean hasPermission() {
        return permission != null && !permission.isEmpty();
    }

    public boolean hasClickable() {
        return clickableText != null && !clickableText.isEmpty();
    }

    public boolean hasClickableUrl() {
        return clickableUrl != null && !clickableUrl.isEmpty();
    }

    public boolean hasSound() {
        return sound != null && !sound.isEmpty();
    }

    public String getDisplayInfo() {
        return "§6ID: §e" + id + " §7- §6Palavras: §e" +
            String.join("§7, §e", keywords) +
            " §7- §6Respostas: §e" + responses.size() +
            " §7- §6Ativacoes: §e" + metricsCount;
    }
}
