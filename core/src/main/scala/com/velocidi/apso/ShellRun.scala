package com.velocidi.apso

/**
 * Wrapper around Scala's process library that facilitates the launching of shell commands.
 */
@deprecated("This will be removed in a future version", "2017/07/13")
case object ShellRun {

  /**
   * Launches an executable with the given arguments.
   * @param ex the executable to run
   * @param args the arguments passed to the executable
   * @return the output of the process as a string
   */
  def apply(ex: String, args: String*): String = sys.process.Process(ex, args).!!

  /**
   * Evaluates and executes a bash expression. By evaluating an expression, bash-specific features
   * such as wildcards, variable replacement and sub-commands are considered. This method requires a
   * bash shell in the system.
   * @param expr the bash expression to evaluate
   * @return the output of the expression as a string
   */
  def eval(expr: String): String = apply("bash", "-c", "eval \"%s\"".format(expr))
}
