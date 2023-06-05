#!/usr/local/bin/python
#/*************************** The CRY.ME project (2023) *************************************************
# *
# *  This file is part of the CRY.ME project (https://github.com/ANSSI-FR/cry-me).
# *  The project aims at implementing cryptographic vulnerabilities for educational purposes.
# *  Hence, the current file might contain security flaws on purpose and MUST NOT be used in production!
# *  Please do not use this source code outside this scope, or use it knowingly.
# *
# *  Many files come from the Android element (https://github.com/vector-im/element-android), the
# *  Matrix SDK (https://github.com/matrix-org/matrix-android-sdk2) as well as the Android Yubikit
# *  (https://github.com/Yubico/yubikit-android) projects and have been willingly modified
# *  for the CRY.ME project purposes. The Android element, Matrix SDK and Yubikit projects are distributed
# *  under the Apache-2.0 license, and so is the CRY.ME project.
# *
# ***************************  (END OF CRY.ME HEADER)   *************************************************/
#

import codecs
import glob
import os
import platform
import subprocess
import sys

import jinja2


# Utility functions
def log(txt):
    print(txt, file=sys.stderr)


def error(txt):
    log(txt)
    sys.exit(2)


def convert(src, dst, environ):
    """Generate a file from a template

    Args:
        src (str): path to input file
        dst (str): path to file to write
        environ (dict): environment dictionary, for replacement mappings.
    """
    with open(src) as infile:
        template = infile.read()
    rendered = jinja2.Template(template).render(**environ)
    with open(dst, "w") as outfile:
        outfile.write(rendered)


def generate_config_from_template(config_dir, config_path, environ, ownership):
    """Generate a homeserver.yaml from environment variables

    Args:
        config_dir (str): where to put generated config files
        config_path (str): where to put the main config file
        environ (dict): environment dictionary
        ownership (str|None): "<user>:<group>" string which will be used to set
            ownership of the generated configs. If None, ownership will not change.
    """
    for v in ("SYNAPSE_SERVER_NAME", "SYNAPSE_REPORT_STATS"):
        if v not in environ:
            error(
                "Environment variable '%s' is mandatory when generating a config file."
                % (v,)
            )

    # populate some params from data files (if they exist, else create new ones)
    environ = environ.copy()
    secrets = {
        "registration": "SYNAPSE_REGISTRATION_SHARED_SECRET",
        "macaroon": "SYNAPSE_MACAROON_SECRET_KEY",
    }

    for name, secret in secrets.items():
        if secret not in environ:
            filename = "/data/%s.%s.key" % (environ["SYNAPSE_SERVER_NAME"], name)

            # if the file already exists, load in the existing value; otherwise,
            # generate a new secret and write it to a file

            if os.path.exists(filename):
                log("Reading %s from %s" % (secret, filename))
                with open(filename) as handle:
                    value = handle.read()
            else:
                log("Generating a random secret for {}".format(secret))
                value = codecs.encode(os.urandom(32), "hex").decode()
                with open(filename, "w") as handle:
                    handle.write(value)
            environ[secret] = value

    environ["SYNAPSE_APPSERVICES"] = glob.glob("/data/appservices/*.yaml")
    if not os.path.exists(config_dir):
        os.mkdir(config_dir)

    # Convert SYNAPSE_NO_TLS to boolean if exists
    if "SYNAPSE_NO_TLS" in environ:
        tlsanswerstring = str.lower(environ["SYNAPSE_NO_TLS"])
        if tlsanswerstring in ("true", "on", "1", "yes"):
            environ["SYNAPSE_NO_TLS"] = True
        else:
            if tlsanswerstring in ("false", "off", "0", "no"):
                environ["SYNAPSE_NO_TLS"] = False
            else:
                error(
                    'Environment variable "SYNAPSE_NO_TLS" found but value "'
                    + tlsanswerstring
                    + '" unrecognized; exiting.'
                )

    if "SYNAPSE_LOG_CONFIG" not in environ:
        environ["SYNAPSE_LOG_CONFIG"] = config_dir + "/log.config"

    log("Generating synapse config file " + config_path)
    convert("/conf/homeserver.yaml", config_path, environ)

    log_config_file = environ["SYNAPSE_LOG_CONFIG"]
    log("Generating log config file " + log_config_file)
    convert("/conf/log.config", log_config_file, environ)

    # Hopefully we already have a signing key, but generate one if not.
    args = [
        "python",
        "-m",
        "synapse.app.homeserver",
        "--config-path",
        config_path,
        # tell synapse to put generated keys in /data rather than /compiled
        "--keys-directory",
        config_dir,
        "--generate-keys",
    ]

    if ownership is not None:
        log(f"Setting ownership on /data to {ownership}")
        subprocess.check_output(["chown", "-R", ownership, "/data"])
        args = ["gosu", ownership] + args

    subprocess.check_output(args)


def run_generate_config(environ, ownership):
    """Run synapse with a --generate-config param to generate a template config file

    Args:
        environ (dict): env var dict
        ownership (str|None): "userid:groupid" arg for chmod. If None, ownership will not change.

    Never returns.
    """
    for v in ("SYNAPSE_SERVER_NAME", "SYNAPSE_REPORT_STATS"):
        if v not in environ:
            error("Environment variable '%s' is mandatory in `generate` mode." % (v,))

    server_name = environ["SYNAPSE_SERVER_NAME"]
    config_dir = environ.get("SYNAPSE_CONFIG_DIR", "/data")
    config_path = environ.get("SYNAPSE_CONFIG_PATH", config_dir + "/homeserver.yaml")
    data_dir = environ.get("SYNAPSE_DATA_DIR", "/data")

    if ownership is not None:
        # make sure that synapse has perms to write to the data dir.
        log(f"Setting ownership on {data_dir} to {ownership}")
        subprocess.check_output(["chown", ownership, data_dir])

    # create a suitable log config from our template
    log_config_file = "%s/%s.log.config" % (config_dir, server_name)
    if not os.path.exists(log_config_file):
        log("Creating log config %s" % (log_config_file,))
        convert("/conf/log.config", log_config_file, environ)

    # generate the main config file, and a signing key.
    args = [
        "python",
        "-m",
        "synapse.app.homeserver",
        "--server-name",
        server_name,
        "--report-stats",
        environ["SYNAPSE_REPORT_STATS"],
        "--config-path",
        config_path,
        "--config-directory",
        config_dir,
        "--data-directory",
        data_dir,
        "--generate-config",
        "--open-private-ports",
    ]
    # log("running %s" % (args, ))
    os.execv("/usr/local/bin/python", args)


def main(args, environ):
    mode = args[1] if len(args) > 1 else "run"

    # if we were given an explicit user to switch to, do so
    ownership = None
    if "UID" in environ:
        desired_uid = int(environ["UID"])
        desired_gid = int(environ.get("GID", "991"))
        ownership = f"{desired_uid}:{desired_gid}"
    elif os.getuid() == 0:
        # otherwise, if we are running as root, use user 991
        ownership = "991:991"

    synapse_worker = environ.get("SYNAPSE_WORKER", "synapse.app.homeserver")

    # In generate mode, generate a configuration and missing keys, then exit
    if mode == "generate":
        return run_generate_config(environ, ownership)

    if mode == "migrate_config":
        # generate a config based on environment vars.
        config_dir = environ.get("SYNAPSE_CONFIG_DIR", "/data")
        config_path = environ.get(
            "SYNAPSE_CONFIG_PATH", config_dir + "/homeserver.yaml"
        )
        return generate_config_from_template(
            config_dir, config_path, environ, ownership
        )

    if mode != "run":
        error("Unknown execution mode '%s'" % (mode,))

    args = args[2:]

    if "-m" not in args:
        args = ["-m", synapse_worker] + args

    jemallocpath = "/usr/lib/%s-linux-gnu/libjemalloc.so.2" % (platform.machine(),)

    if os.path.isfile(jemallocpath):
        environ["LD_PRELOAD"] = jemallocpath
    else:
        log("Could not find %s, will not use" % (jemallocpath,))

    # if there are no config files passed to synapse, try adding the default file
    if not any(p.startswith("--config-path") or p.startswith("-c") for p in args):
        config_dir = environ.get("SYNAPSE_CONFIG_DIR", "/data")
        config_path = environ.get(
            "SYNAPSE_CONFIG_PATH", config_dir + "/homeserver.yaml"
        )

        if not os.path.exists(config_path):
            if "SYNAPSE_SERVER_NAME" in environ:
                error(
                    """\
Config file '%s' does not exist.

The synapse docker image no longer supports generating a config file on-the-fly
based on environment variables. You can migrate to a static config file by
running with 'migrate_config'. See the README for more details.
"""
                    % (config_path,)
                )

            error(
                "Config file '%s' does not exist. You should either create a new "
                "config file by running with the `generate` argument (and then edit "
                "the resulting file before restarting) or specify the path to an "
                "existing config file with the SYNAPSE_CONFIG_PATH variable."
                % (config_path,)
            )

        args += ["--config-path", config_path]

    log("Starting synapse with args " + " ".join(args))

    args = ["python"] + args
    if ownership is not None:
        args = ["gosu", ownership] + args
        os.execve("/usr/sbin/gosu", args, environ)
    else:
        os.execve("/usr/local/bin/python", args, environ)


if __name__ == "__main__":
    main(sys.argv, os.environ)
