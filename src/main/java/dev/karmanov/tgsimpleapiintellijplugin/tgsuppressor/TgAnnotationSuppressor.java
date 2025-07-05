package dev.karmanov.tgsimpleapiintellijplugin.tgsuppressor;

import com.intellij.codeInspection.InspectionSuppressor;
import com.intellij.codeInspection.SuppressQuickFix;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.psi.*;
import com.intellij.psi.util.PsiTreeUtil;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public class TgAnnotationSuppressor implements InspectionSuppressor {

    private static final Logger LOG = Logger.getInstance(TgAnnotationSuppressor.class);

    private static final Set<String> TG_ANNOTATIONS = new HashSet<>(Arrays.asList(
            "dev.karmanov.library.annotation.botActivity.RoleBasedAccess",
            "dev.karmanov.library.annotation.userActivity.BotCallBack",
            "dev.karmanov.library.annotation.userActivity.BotDocument",
            "dev.karmanov.library.annotation.userActivity.BotLocation",
            "dev.karmanov.library.annotation.userActivity.BotMedia",
            "dev.karmanov.library.annotation.userActivity.BotPhoto",
            "dev.karmanov.library.annotation.userActivity.BotScheduled",
            "dev.karmanov.library.annotation.userActivity.BotText",
            "dev.karmanov.library.annotation.userActivity.BotVoice"
    ));

    @Override
    public boolean isSuppressedFor(@NotNull PsiElement element, @NotNull String toolId) {
        LOG.debug("isSuppressedFor: toolId='{}'", toolId);

        if (!"UNUSED_SYMBOL".equals(toolId)) {
            return false;
        }

        PsiModifierListOwner owner = PsiTreeUtil.getParentOfType(element, PsiModifierListOwner.class);
        if (!(owner instanceof PsiMethod)) {
            return false;
        }

        boolean hasTg = Arrays.stream(owner.getModifierList().getAnnotations())
                .map(PsiAnnotation::getQualifiedName)
                .anyMatch(qName -> qName != null && TG_ANNOTATIONS.contains(qName));

        if (hasTg) {
            String name = ((PsiNamedElement) owner).getName();
            LOG.debug("Suppressing '{}' on '{}'", toolId, name);
            return true;
        }

        return false;
    }

    @Override
    public SuppressQuickFix @NotNull [] getSuppressActions(PsiElement element, @NotNull String toolId) {
        return SuppressQuickFix.EMPTY_ARRAY;
    }
}
