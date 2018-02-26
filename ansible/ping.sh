#!/bin/bash
ansible -i "$1" all -e 'host_key_checking=False' -m ping