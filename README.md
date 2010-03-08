# dynclj

dynclj is a small script to update DynDNS.org hosts. Written in [Clojure][1],
it was meant as Clojure learning experience for me.

## Quick Start

0. install [leiningen][2]
1. in your dynclj checkout, run `lein deps` followed by `lein compile`
3. create the config file ~/.dynclj/dynclj.conf with three lines:

    username=<username>     # dyndns username
    password=<password>     # password matching above username
    hosts=<host1>,<host2>   # comma-separated list of hosts to update

4. run dynclj.sh

## How it Works

1. check config file, determine hosts to update
2. obtain the current IP address from checkip.dyndns.org
3. check cache file (~/.dynclj/dynclj.cache) to determine when each host was
   last updated, if at all
4. update hosts if required

## Command-line Arguments

Run ./dynclj.sh --help to see the available command-line arguments. From the
help text:

  --config-file, -c <arg>  use specified configuration file
  --ip <arg>               force update to use this IP
  --force                  force the update
  --verbose, -v            verbose output

 [1]: http://clojure.org
 [2]: http://github.com/technomancy/leiningen
