package ru.golchin.roman.calculator;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

public class MainActivity extends AppCompatActivity implements Button.OnClickListener {

    private static final String DIGITS = "0123456789";
    private static final String OPERATORS = "+-*/";
    private static final int MAX_MAIN_LENGTH = 10;
    private static final int MAX_INTERMEDIATE_LENGTH = 20;
    private String decimalSeparator;
    private String initialValue;
    private String errorMessage;

    private String pendingOperation;
    private TextView mainDisplay;
    private TextView intermediateDisplay;
    private Double result = 0.0;
    private Double rightOperand;
    //true iff current number contains separator (to prevent multiple separators)
    private boolean isFractional;
    //true iff calculator is getting number
    private boolean numberStarted;
    // true iff last operation is +/-
    private boolean lowPriority;
    private StringBuilder numberBuilder = new StringBuilder();
//    private StringBuilder resultBuilder = new StringBuilder();


    private String shortenDouble(Double x, int length) {
        return String.valueOf(x);
    }

    private void calculate() {
        if (pendingOperation == null || rightOperand == null)
            return;
        if (pendingOperation.equals("+")) {
            result += rightOperand;
        } else if (pendingOperation.equals("-")) {
            result -= rightOperand;
        } else if (pendingOperation.equals("*")) {
            result *= rightOperand;
        } else if (pendingOperation.equals("/")) {
            result /= rightOperand;
        }
        if (Double.isNaN(result) || Double.isInfinite(result)) {
            intermediateDisplay.setText("");
            mainDisplay.setText(errorMessage);
            result = 0.0;
        }
        mainDisplay.setText(shortenDouble(result, MAX_MAIN_LENGTH));
    }

    @Override
    public void onClick(View view) {
        final String GENERAL_TAG = "onClick";
        String command = ((Button) view).getText().toString();
        Log.d(GENERAL_TAG, command);
        if (command.equals("C")) {
            mainDisplay.setText(initialValue);
            intermediateDisplay.setText("");
            reset();
            return;
        }
        if (command.equals("=")) {
            if (pendingOperation != null) {
                try {
                    rightOperand = Double.parseDouble(numberBuilder.toString());
                } catch (NumberFormatException e) {
                    rightOperand = 0.;
                }
                calculate();
            }
            intermediateDisplay.setText("");
            reset();
            return;
        }
        int mainLength = mainDisplay.getText().length();
        if (command.equals("DEL")) {
            if (mainLength == 1) {
                mainDisplay.setText(initialValue);
            } else {
                mainDisplay.setText(mainDisplay.getText().subSequence(0, mainLength - 2));
            }
        }
        int intermediateLength = intermediateDisplay.getText().length();
        if (mainLength >= MAX_MAIN_LENGTH) {
            return;
        }
        //the following operations are expected to increase the length of text in one or both displays
        //if length of main display is exceeded calculator waits for command other than digit or separator
        //if length of intermediate display is exceeded result is copied from main display to intermediate one
        //and operation is appended to it
        //for example, if intermediate = '(123+456)*789', command = '/' then we get '456831/'
        //the result of calculation is guaranteed to fit in main display and intermediate display has more capacity
        if (OPERATORS.contains(command)) {
            if (pendingOperation != null) {
                if (rightOperand == null)
                    return;
                calculate();
            } else {
                try {
                    result = Double.parseDouble(numberBuilder.toString());
                } catch (NumberFormatException e) {
                    result = 0.;
                }
            }
            pendingOperation = command;
            numberStarted = false;
            //check whether we need to add parentheses to enforce correctness of the expression
            boolean addParen = lowPriority && (command.equals("*") || command.equals("/"));
            int additionLength = addParen ? 3 : 1;
            if (intermediateLength + additionLength > MAX_INTERMEDIATE_LENGTH) {
                intermediateDisplay.setText(mainDisplay.getText());
            } else if (addParen) {
                intermediateDisplay.setText("(" + intermediateDisplay.getText().toString() + ")");
                lowPriority = false;
            }
            intermediateDisplay.append(command);
            lowPriority = (command.equals("+") || command.equals("-"));
        }

        if (DIGITS.contains(command)) {
            if (numberStarted || !command.equals("0")) {
                numberBuilder.append(command);
            }
            if (numberStarted) {
                mainDisplay.append(command);
                numberBuilder.append(command);
            } else if (!command.equals("0")) {
                mainDisplay.setText(command);
                numberStarted = true;
            }
        }
        if (command.equals(decimalSeparator)) {
            if (numberStarted && !isFractional) {
                mainDisplay.append(decimalSeparator);
                isFractional = true;
            }
            if (!numberStarted) {
                mainDisplay.setText("0" + decimalSeparator);
                numberStarted = true;
                isFractional = true;
            }
        }
        numberBuilder.setLength(0);
        numberBuilder.append(mainDisplay.getText());
        Log.d("Result ", String.valueOf(result));
        Log.d("Intermediate ", numberBuilder.toString());
        Log.d("numberStarted ", String.valueOf(numberStarted));
        Log.d("isFractional ", String.valueOf(isFractional));
        Log.d("lowPriority ", String.valueOf(lowPriority));

    }


    //invoked on start, after tapping C or =
    private void reset() {
        lowPriority = isFractional = numberStarted = false;
        pendingOperation = null;
        result = 0.;
    }

    private void setOnClickListenerGroup(ViewGroup viewGroup) {
        for (int i = 0; i < viewGroup.getChildCount(); ++i) {
            View item = viewGroup.getChildAt(i);
            if (item instanceof ViewGroup)
                setOnClickListenerGroup((ViewGroup) item);
            else
                item.setOnClickListener(this);
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        decimalSeparator = getString(R.string.decimal_separator);
        mainDisplay = (TextView) findViewById(R.id.main);
        intermediateDisplay = (TextView) findViewById(R.id.intermediate);
        reset();
        initialValue = getString(R.string.initial_value);
        errorMessage = getString(R.string.error_message);
        ViewGroup buttons = (ViewGroup) findViewById(R.id.buttons);
        setOnClickListenerGroup(buttons);
    }
}
