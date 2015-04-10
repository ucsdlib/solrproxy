<%
    String version = application.getInitParameter("version-number");
    if ( version == null || version.trim().equals("") )
    {
            version = "0.0.0";
    }
    String build = application.getInitParameter("build-date");
    if ( build == null || build.trim().equals("") )
    {
            build = "unknown";
    }
    String branch = application.getInitParameter("build-branch");
    if ( branch == null || branch.trim().equals("") )
    {
        branch = "unknown";
    }
%><html>
  <head>
    <title>SolrProxy Version <%=version%> (<%=build%>)</title>
  </head>
  <body>
    <p>SolrProxy Version <%=version%>, Build <%=build%></p>
    <p>Built from: <%=branch%></p>
  </body>
</html>
