package com.smartplate.app;

import com.smartplate.ai.AIModelFactory;

import java.util.Locale;

/**
 * The command-line options of SmartPlateApp, parsed into one immutable value.
 *
 * <pre>
 *   (no options)               desktop GUI (the CLI if there is no display)
 *   --cli                      interactive command-line interface
 *   --input FILE               run the CLI commands in FILE, show each result, then exit
 *   --input -                  the same, reading the commands from standard input
 *   --provider NAME            AI model: mock, anthropic, gemini or ollama (overrides .env)
 *   --mock                     same as --provider mock
 *   --empty                    start with an empty pantry instead of the sample pantry
 *   --help                     print this list and exit
 * </pre>
 * An unknown option or a missing value is an error, so a typo is never silently ignored.
 */
public final class CommandLineOptions {

    public static final String USAGE = String.join("\n",
            "Usage: java -jar smartplate-1.0.0.jar [options]",
            "",
            "  (no options)        desktop GUI (falls back to the CLI if there is no display)",
            "  --cli               interactive command-line interface",
            "  --input FILE        run the CLI commands in FILE (one per line, # starts a comment),",
            "                      print each command and its result, then exit",
            "  --input -           the same, reading the commands from standard input",
            "  --provider NAME     AI model: mock, anthropic, gemini or ollama (overrides",
            "                      SMARTPLATE_AI_PROVIDER in .env; keys and model names still come from .env)",
            "  --mock              same as --provider mock (offline, no key needed)",
            "  --empty             start with an empty pantry instead of the sample pantry",
            "  --help              show this help and exit",
            "",
            "Example: java -jar target/smartplate-1.0.0.jar --provider ollama --input examples/task.txt");

    private final boolean cli;
    private final String inputFile;
    private final String provider;
    private final boolean emptyPantry;
    private final boolean help;

    private CommandLineOptions(boolean cli, String inputFile, String provider, boolean emptyPantry, boolean help) {
        this.cli = cli;
        this.inputFile = inputFile;
        this.provider = provider;
        this.emptyPantry = emptyPantry;
        this.help = help;
    }

    /** Parses the arguments; throws IllegalArgumentException with a readable message on a bad option. */
    public static CommandLineOptions parse(String... args) {
        boolean cli = false;
        String inputFile = null;
        String provider = null;
        boolean empty = false;
        boolean help = false;
        for (int i = 0; i < args.length; i++) {
            String arg = args[i];
            switch (arg) {
                case "--cli":
                    cli = true;
                    break;
                case "--input":
                    inputFile = value(args, ++i, arg);
                    cli = true;
                    break;
                case "--provider":
                    provider = providerName(value(args, ++i, arg));
                    break;
                case "--mock":
                    provider = AIModelFactory.MOCK;
                    break;
                case "--empty":
                    empty = true;
                    break;
                case "--help":
                case "-h":
                    help = true;
                    break;
                default:
                    throw new IllegalArgumentException("Unknown option '" + arg + "'. Use --help to see the options.");
            }
        }
        return new CommandLineOptions(cli, inputFile, provider, empty, help);
    }

    private static String value(String[] args, int index, String option) {
        if (index >= args.length || args[index].startsWith("--")) {
            throw new IllegalArgumentException(option + " needs a value. Use --help to see the options.");
        }
        return args[index];
    }

    private static String providerName(String name) {
        String key = name.trim().toLowerCase(Locale.ROOT);
        if (!AIModelFactory.PROVIDERS.contains(key)) {
            throw new IllegalArgumentException("Unknown provider '" + name + "'. Use one of "
                    + String.join(", ", AIModelFactory.PROVIDERS) + ".");
        }
        return key;
    }

    /** True for --cli or --input. */
    public boolean isCli() {
        return cli;
    }

    /** The task file, "-" for standard input, or null for the interactive CLI / GUI. */
    public String getInputFile() {
        return inputFile;
    }

    /** The provider chosen on the command line, or null to let the environment (.env) decide. */
    public String getProvider() {
        return provider;
    }

    public boolean isEmptyPantry() {
        return emptyPantry;
    }

    public boolean isHelp() {
        return help;
    }
}
