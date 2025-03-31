package com.spotifyxp.args;

import com.spotifyxp.utils.ApplicationUtils;

import java.util.ArrayList;

public class ArgParser {
    /**
     * Holds all passed arguments
     */
    public static final ArrayList<Argument> passedArguments = new ArrayList<>();
    /**
     * Holds all registered arguments
     */
    public final ArrayList<com.spotifyxp.args.Argument> arguments = new ArrayList<>();

    /**
     * Holds information about an argument
     */
    public static class Argument {
        private com.spotifyxp.args.Argument argument;
        private String name;
        private String description;
        private boolean hasParameter;
        private String parameter;

        /**
         * Executes the code specified in the argument
         */
        public void execute() {
            argument.runArgument(parameter);
        }

        /**
         * Returns the name of the argument
         *
         * @return String
         */
        public String getName() {
            return name;
        }

        /**
         * Returns the description of the argument
         *
         * @return String
         */
        public String getDescription() {
            return description;
        }

        /**
         * Returns the parameter given to the argument (if hasParameter is true)<br>
         * Example: myargument=xyz
         *
         * @return String
         */
        public String getParameter() {
            return parameter;
        }

        /**
         * Returns if the argument has a parameter
         *
         * @return Boolean
         */
        public boolean hasParameter() {
            return hasParameter;
        }
    }

    public ArgParser() {
        arguments.add(new CustomSaveDir());
        arguments.add(new Debug());
        arguments.add(new Development());
        arguments.add(new Language());
        arguments.add(new LogViewer());
        arguments.add(new NoMediaControl());
        arguments.add(new RunUpdater());
        arguments.add(new SaveLog());
        arguments.add(new SetupComplete());
        arguments.add(new Help());
    }

    /**
     * Prints an argument help containing a list of all commands available
     */
    public void printHelp() {
        System.out.println("SpotifyXP - " + ApplicationUtils.getVersion() + "\n");
        System.out.println("Usage java -jar SpotifyXP.jar <argument>..." + "\n\n");
        for (com.spotifyxp.args.Argument a : arguments) {
            if (a.hasParameter()) {
                System.out.println("--" + a.getName() + "=<parameter>" + "   =>   " + a.getDescription());
            } else {
                System.out.println("--" + a.getName() + "   =>   " + a.getDescription());
            }
        }
    }

    /**
     * Parses all the given arguments
     *
     * @param args Array of all arguments passed to SpotifyXP
     */
    public void parseArguments(String[] args) {
        boolean isvalid = false;
        for (String s : args) {
            String argument = s.replace("--", "");
            try {
                argument = s.replace("--", "").split("=")[0];
            } catch (Exception ignored) {
            }
            String parameter = "";
            try {
                parameter = s.replace(s.split("=")[0] + "=", "");
            } catch (Exception ignored) {
            }
            for (com.spotifyxp.args.Argument a : arguments) {
                if (a.getName().equals(argument)) {
                    isvalid = true;
                    a.runArgument(parameter).run();
                    Argument arg = new Argument();
                    arg.hasParameter = a.hasParameter();
                    arg.argument = a;
                    arg.name = a.getName();
                    arg.description = a.getDescription();
                    arg.parameter = parameter;
                    passedArguments.add(arg);
                    break;
                }
            }
            if (!isvalid) {
                System.out.println("Invalid argument: " + argument + "\n");
                printHelp();
            }
        }
    }
}
