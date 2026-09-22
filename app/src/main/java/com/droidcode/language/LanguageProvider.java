package com.droidcode.language;

import java.util.List;

public interface LanguageProvider {
    String getLanguageId();
    String getDisplayName();
    List<String> getKeywords();
    String getSingleLineCommentPrefix();
}
