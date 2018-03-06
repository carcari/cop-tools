Tools for Copernicus Products


# PostgreSQL setup (For RedHat/CentOS 7)

##After installing PostgreSQL 9.6 server, perform the following commands:

###For sudoers:

sudo /usr/pgsql-9.6/bin/postgresql96-setup initdb

sudo systemctl start postgresql-9.6.service

sudo systemctl enable postgresql-9.6.service

###For NOT sudoers:

su -c '/usr/pgsql-9.6/bin/postgresql96-setup initdb'

su -c 'systemctl start postgresql-9.6.service'

su -c 'systemctl enable postgresql-9.6.service'

(TODO: Add steps with creation of user, database, schema and pg_hba.conf, etc...)