package ru.golchin.roman.calculator;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import static ru.golchin.roman.calculator.R.id.intermediate;

public class MainActivity extends AppCompatActivity implements Button.OnClickListener {

    private static final String DIGITS = "0123456789";
    private static final String OPERATORS = "+-*/";
    private static final int MAX_MAIN_LENGTH = 10;
    private static final int MAX_INTERMEDIATE_LENGTH = 20;
    private String decimalSeparator;
    private String initialValue;
    private String errorMessage;

    private String pendingOperator;
    private TextView mainDisplay;
    private TextView intermediateDisplay;
    private Double result = 0.0;
    private Double rightOperand;
    //true when current number contains separator (to prevent multiple separators)
    private boolean isFractional;
    //true when calculator is getting number
    private boolean gettingNumber;
    // true when last operator is +/-
    private boolean lowPriority;
    //true when calculator is getting operator, current operator wipes out the previous one; it means that there's no right operand yet
    private boolean gettingOperator;
    private boolean addParen;
    private StringBuilder numberBuilder = new StringBuilder();


    private String shortenDouble(Double x, int length) {
        String res = String.valueOf(x);
        int len = res.length();
        return (len >= 2 && res.charAt(len - 2) == decimalSeparator.charAt(0) && res.charAt(len - 1) == '0') ? res.substring(0, len - 2) : res;
    }

    private void calculate() {
        if (pendingOperator == null) {
        }
        if (rightOperand == null) {
            return;
        }
        if (pendingOperator.equals("+")) {
            result += rightOperand;
        } else if (pendingOperator.equals("-")) {
            result -= rightOperand;
        } else if (pendingOperator.equals("*")) {
            result *= rightOperand;
        } else if (pendingOperator.equals("/")) {
            result /= rightOperand;
        }
        if (Double.isNaN(result) || Double.isInfinite(result)) {
            intermediateDisplay.setText("");
            mainDisplay.setText(errorMessage);
            result = 0.0;
        }
        if (!mainDisplay.getText().toString().equals(errorMessage)) {
            mainDisplay.setText(shortenDouble(result, MAX_MAIN_LENGTH));
        }

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
            result = 0.;
            return;
        }
        //if an error occurred typing an operator is not allowed but user can start typing a new number
        boolean error = mainDisplay.getText().toString().equals(errorMessage);
        if (!error && command.equals("=")) {
            if (numberBuilder.charAt(numberBuilder.length() - 1) == decimalSeparator.charAt(0))
                mainDisplay.setText(numberBuilder.subSequence(0, numberBuilder.length() - 1));
            if (pendingOperator != null) {
                if (!gettingOperator) {
                    try {
                        rightOperand = Double.parseDouble(numberBuilder.toString());
                    } catch (NumberFormatException e) {
                        rightOperand = 0.;
                    }
                } else
                    rightOperand = result;
                calculate();
            } else {
                try {
                    result = Double.parseDouble(numberBuilder.toString());
                } catch (NumberFormatException e) {
                    result = 0.;
                }
            }
            intermediateDisplay.setText("");
            reset();
            return;
        }
        int mainLength = mainDisplay.getText().length();
        if (!error && command.equals("DEL")) {
            if (gettingNumber) {
                if (mainLength == 1) {
                    mainDisplay.setText(initialValue);
                } else {
                    mainDisplay.setText(mainDisplay.getText().subSequence(0, mainLength - 1));
                }
            }
        }
        int intermediateLength = intermediateDisplay.getText().length();
        if (mainLength >= MAX_MAIN_LENGTH) {
            return;
        }
        //the following operations are expected to increase the length of text in one or both displays
        //if length of main display is exceeded calculator waits for command other than digit or separator
        //if length of intermediate display is exceeded result is copied from main display to intermediate one
        //and operator is appended to it
        //for example, if intermediate = '(123+456)*789', command = '/' then we get '456831/'
        //the result of calculation is guaranteed to fit in main display and intermediate display has more capacity
        if (!error && OPERATORS.contains(command)) {
            boolean prevAddParen = false;
            // this is not the first operator
            if (gettingOperator) {
                prevAddParen = addParen;
                intermediateDisplay.setText(intermediateDisplay.getText().subSequence(0, intermediateLength - 1));
                --intermediateLength;
            }
            //check whether we need to add parentheses to enforce correctness of the expression
            addParen = lowPriority && (command.equals("*") || command.equals("/"));
            if (!gettingOperator)
                lowPriority = (pendingOperator == null || pendingOperator.equals("+") || pendingOperator.equals("-"));
            //handle the situation when previous operator had low priority (and required parentheses) and the current one has high priority
            if (gettingOperator && prevAddParen && !addParen) {
                intermediateDisplay.setText(intermediateDisplay.getText().subSequence(1, intermediateLength - 1));
            }
            //remove unneeded separator in integer
            if (gettingNumber && numberBuilder.charAt(numberBuilder.length() - 1) == decimalSeparator.charAt(0)) {
                numberBuilder.setLength(numberBuilder.length() - 1);
                mainDisplay.setText(mainDisplay.getText().subSequence(0, mainLength - 1));
            }
            //add operator and right operand
            int additionLength = 1 + numberBuilder.length();
            additionLength += addParen ? 2 : 0;
            boolean shouldShorten = (intermediateLength + additionLength > MAX_INTERMEDIATE_LENGTH);
            Log.d("shouldShorten ", String.valueOf(shouldShorten));
            if (!shouldShorten && !gettingOperator) {
                intermediateDisplay.append(mainDisplay.getText());
            }
            //operation that should be completed before the current one
            if (pendingOperator != null) {
                if (gettingOperator) {
                    pendingOperator = null;
                } else {
                    try {
                        rightOperand = Double.parseDouble(numberBuilder.toString());
                    } catch (NumberFormatException e) {
                        rightOperand = 0.;
                    }
                    calculate();
                }
            } else {
                try {
                    result = Double.parseDouble(mainDisplay.getText().toString());
                } catch (NumberFormatException e) {
                    result = 0.;
                }
            }
            if (mainDisplay.getText().toString().equals(errorMessage)) {
                return;
            }
            gettingOperator = true;
            pendingOperator = command;
            if (shouldShorten) {
                intermediateDisplay.setText(mainDisplay.getText());
            }
            if (addParen) {
                intermediateDisplay.setText("(" + intermediateDisplay.getText().toString() + ")");
            }
            intermediateDisplay.append(command);
            gettingNumber = false;
            isFractional = false;
        }

        if (DIGITS.contains(command)) {
            //several zeros at start are not allowed
            gettingOperator = false;
            if (!gettingNumber) {
                numberBuilder.setLength(0);
                numberBuilder.append(command);
                gettingNumber = true;
            } else {
                if (!numberBuilder.toString().equals("0"))
                    numberBuilder.append(command);
                else if (!command.equals("0")) {
                    numberBuilder.setLength(0);
                    numberBuilder.append(command);
                } else {
                    return;
                }
            }
            mainDisplay.setText(numberBuilder);
            return;
        }
        if (command.equals(decimalSeparator)) {
            gettingOperator = false;
            if (gettingNumber && !isFractional) {
                numberBuilder.append(decimalSeparator);
                isFractional = true;
            }
            //omitting zero is allowed
            if (!gettingNumber) {
                numberBuilder.setLength(0);
                numberBuilder.append("0").append(decimalSeparator);
                gettingNumber = true;
                isFractional = true;
            }
            mainDisplay.setText(numberBuilder);
        }
        numberBuilder.setLength(0);
        numberBuilder.append(mainDisplay.getText());
        Log.d("Result ", String.valueOf(result));
        Log.d("Number ", numberBuilder.toString());
        Log.d("gettingNumber ", String.valueOf(gettingNumber));
        Log.d("isFractional ", String.valueOf(isFractional));
        Log.d("lowPriority ", String.valueOf(lowPriority));
        Log.d("pendingOperator ", pendingOperator == null ? "null" : pendingOperator);
        Log.d("rightOperand", rightOperand == null ? "null" : String.valueOf(rightOperand));
    }


    //invoked on start, after tapping C or =
    private void reset() {
        addParen = gettingOperator = lowPriority = isFractional = gettingNumber = false;
        pendingOperator = null;
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
        intermediateDisplay = (TextView) findViewById(intermediate);
        reset();
        result = 0.;
        initialValue = getString(R.string.initial_value);
        errorMessage = getString(R.string.error_message);
        ViewGroup buttons = (ViewGroup) findViewById(R.id.buttons);
        setOnClickListenerGroup(buttons);
    }
}
