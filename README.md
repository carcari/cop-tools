Tools for Copernicus Products


# PostgreSQL setup (For RedHat/CentOS 7)

## After installing PostgreSQL 9.6 server, perform the following commands as superuser:

### Initialize database

`/usr/pgsql-9.6/bin/postgresql96-setup initdb`

### Enable PostgreSQL as service and start it

`systemctl start postgresql-9.6.service`

`systemctl enable postgresql-9.6.service`


### Update the HBA configuration file to allow password authentication

Open the  HBA configuration file (please note that the path may change on the basis of PostgreSQL version)

`vi /var/lib/pgsql/9.6/data/pg_hba.conf`

Find the lines that looks like this, near the bottom of the file:

```
host    all             all             127.0.0.1/32            ident
host    all             all             ::1/128                 ident
```

Then replace "ident" with "md5", so they look like this:

```
host    all             all             127.0.0.1/32            md5
host    all             all             ::1/128                 md5
```


Save and exit. PostgreSQL is now configured to allow password authentication.

## Create user, database and schema for dias

PostgreSQL server installation creates postgres user, which is the PostgreSQL DBA account.

### Change user to postgres:

`su - postgres`


### Access the postgres prompt immediately by typing:

`psql`

### In the postgres shell, perform the following commands (remember to add __;__ after each command):

```
#psql create user dias with encrypted password '<password>';

#psql create database dias owner dias;

#psql create schema dias authorization dias;
```

### Exit prom psql shell 

`#psql \q`
