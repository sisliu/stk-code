package org.supertuxkart.stk_dbg;

import org.supertuxkart.stk_dbg.STKInputConnection;

import android.content.Context;
import android.text.InputType;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputConnection;
import android.view.inputmethod.InputMethodManager;
import android.view.KeyEvent;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;

// We need to extend EditText instead of view to allow copying to our STK
// editbox
public class STKEditText extends EditText
{
    private int m_composing_start;

    private int m_composing_end;

    /* Used to prevent copying text to non focused widget in STK. */
    private int m_stk_widget_id;

    STKInputConnection m_stk_input_connection;

    /* Used to avoid infinite calling updateSTKEditBox if setText currently
     * by jni. */
    private boolean m_from_stk_editbox;
    // ------------------------------------------------------------------------
    private native static void editText2STKEditbox(int widget_id,
                                                   String full_text, int start,
                                                   int end,
                                                   int composing_start,
                                                   int composing_end);
    // ------------------------------------------------------------------------
    private native static void handleActionNext(int widget_id);
    // ------------------------------------------------------------------------
    public STKEditText(Context context)
    {
        super(context);
        setInputType(InputType.TYPE_CLASS_TEXT);
        setFocusableInTouchMode(true);
        m_composing_start = 0;
        m_composing_end = 0;
        m_stk_widget_id = -1;
        m_from_stk_editbox = false;
        m_stk_input_connection = null;
        setOnEditorActionListener(new EditText.OnEditorActionListener()
        {
        @Override
            public boolean onEditorAction(TextView v, int action_id,
                                          KeyEvent event)
            {
                if (action_id == EditorInfo.IME_ACTION_NEXT)
                {
                    handleActionNext(m_stk_widget_id);
                    // STK will handle the closing of the screen keyboard
                    return true;
                }
                return false;
            }
        });
    }
    // ------------------------------------------------------------------------
    @Override
    public InputConnection onCreateInputConnection(EditorInfo out_attrs)
    {
        if (m_stk_input_connection == null)
        {
            m_stk_input_connection = new STKInputConnection(
                super.onCreateInputConnection(out_attrs), this);
        }
        out_attrs.actionLabel = null;
        out_attrs.inputType = getInputType();
        out_attrs.imeOptions = EditorInfo.IME_ACTION_NEXT |
            EditorInfo.IME_FLAG_NO_FULLSCREEN |
            EditorInfo.IME_FLAG_NO_EXTRACT_UI;
        return m_stk_input_connection;
    }
    // ------------------------------------------------------------------------
    @Override
    public boolean onCheckIsTextEditor()                       { return true; }
    // ------------------------------------------------------------------------
    @Override
    public boolean onKeyPreIme(int key_code, KeyEvent event)
    {
        // Always remove the focus on STKEdit when pressing back button in
        // phone, which hideSoftInputFromWindow is called by java itself
        if (event.getKeyCode() == KeyEvent.KEYCODE_BACK)
            beforeHideKeyboard();
        return false;
    }
    // ------------------------------------------------------------------------
    public void setComposingRegion(int start, int end)
    {
        // From doc of InputConnectionWrapper, it says:
        // Editor authors, be ready to accept a start that is greater than end.
        if (start != end && start > end)
        {
            m_composing_end = start;
            m_composing_start = end;
        }
        else
        {
            m_composing_start = start;
            m_composing_end = end;
        }
    }
    // ------------------------------------------------------------------------
    public void updateSTKEditBox()
    {
        if (!isFocused() || m_from_stk_editbox)
            return;
        editText2STKEditbox(m_stk_widget_id, getText().toString(),
            getSelectionStart(), getSelectionEnd(), m_composing_start,
            m_composing_end);
    }
    // ------------------------------------------------------------------------
    public void beforeHideKeyboard()
    {
        clearFocus();
        setVisibility(View.GONE);
    }
    // ------------------------------------------------------------------------
    /* Called by STK with JNI to set this view with new text (like user focus
     * a new editbox in stk, or change cursor / selection). */
    public void setTextFromSTK(int widget_id, final String text,
                               int selection_start, int selection_end)
    {
        m_stk_widget_id = widget_id;
        // Avoid sending the newly set text back to STK at the same time
        m_from_stk_editbox = true;
        try
        {
            String old_text = getText().toString();
            boolean text_changed = !text.equals(old_text);
            if (text_changed)
            {
                super.clearComposingText();
                super.setText(text);
                m_stk_input_connection.setComposingRegion(0, 0);
            }

            if (selection_start != selection_end &&
                selection_start > selection_end)
            {
                int temp = selection_end;
                selection_end = selection_start;
                selection_start = temp;
            }
            if (selection_start < 0)
                selection_start = 0;
            if (selection_end > length())
                selection_end = length();

            if (text_changed)
            {
                InputMethodManager imm = (InputMethodManager)getContext()
                    .getSystemService(Context.INPUT_METHOD_SERVICE);
                if (imm != null)
                {
                    // From google, You should call this when the text within
                    // your view changes outside of the normal input method or
                    // key input flow, such as when an application calls
                    // TextView.setText().
                    imm.restartInput(this);
                }
            }
            setSelection(selection_start, selection_end);
        }
        catch (Exception e)
        {
            m_from_stk_editbox = false;
        }
        m_from_stk_editbox = false;
    }
    // ------------------------------------------------------------------------
    public STKInputConnection getSTKInputConnection()
                                             { return m_stk_input_connection; }
    // ------------------------------------------------------------------------
    public void configType(final int type)
    {
        // Check text_box_widget.hpp for definition
        switch (type)
        {
        case 0:
            setInputType(InputType.TYPE_CLASS_TEXT);
            break;
        case 1:
            setInputType(InputType.TYPE_TEXT_VARIATION_PASSWORD);
            break;
        case 2:
            setInputType(InputType.TYPE_CLASS_NUMBER);
            break;
        case 3:
            setInputType(InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS);
            break;
        default:
            break;
        }
    }
}
