package net.getnova.backend.maven;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.*;
import lombok.RequiredArgsConstructor;
import net.getnova.backend.codec.http.HttpUtils;
import net.getnova.backend.codec.http.server.HttpLocation;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.Base64;
import java.util.regex.Pattern;

@RequiredArgsConstructor
public class MavenHttpLocation extends HttpLocation<HttpMessage> {

    private final String path;
    private final MavenServerConfig config;

    private HttpRequest request;
    private FileOutputStream outputStream;

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, HttpMessage msg) throws Exception {
        if (msg instanceof HttpRequest) {
            this.request = (HttpRequest) msg;
            if (this.request.method().equals(HttpMethod.GET)) {
                String uri = this.request.uri().replaceFirst(Pattern.quote("/maven"), "");
                File file = new File(path + uri.replace('/', File.separatorChar));
                if (!file.exists()) HttpUtils.sendStatus(ctx, this.request, HttpResponseStatus.NOT_FOUND);
                else if (file.isDirectory()) {
                    StringBuilder index = new StringBuilder();
                    for (File f : file.listFiles()) {
                        index.append("File name: ").append(f.getName()).append("\n");
                        index.append("Size: ").append(f.length()).append("\n\n");
                    }
                    HttpUtils.sendAndCleanupConnection(ctx, this.request, new DefaultHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.OK),
                            new DefaultHttpContent(Unpooled.copiedBuffer(index.toString().getBytes())));
                } else try (final InputStream is = new FileInputStream(file)) {
                    HttpUtils.sendAndCleanupConnection(ctx, this.request, new DefaultHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.OK),
                            new DefaultHttpContent(Unpooled.copiedBuffer(is.readAllBytes())));
                }
            }
            return;
        }

        if (this.request.method().equals(HttpMethod.GET)) return;

        ByteBuf content = ((HttpContent) msg).content();
        boolean last = msg instanceof LastHttpContent;

        String uri = this.request.uri().replaceFirst(Pattern.quote("/maven"), "");
        File file = new File(path + uri.replace('/', File.separatorChar));
        switch (this.request.method().toString().toUpperCase()) {
            case "PUT":
            case "POST":
                if (!msg.headers().contains(HttpHeaderNames.AUTHORIZATION)) {
                    HttpUtils.sendStatus(ctx, this.request, HttpResponseStatus.UNAUTHORIZED);
                    return;
                }
                String authenticationMethod = msg.headers().get(HttpHeaderNames.AUTHORIZATION).split(" ")[0];
                if (!authenticationMethod.equals("Basic")) {
                    HttpUtils.sendStatus(ctx, this.request, HttpResponseStatus.BAD_REQUEST);
                    return;
                }
                String[] credentials = new String(Base64.getDecoder().decode(msg.headers().get(HttpHeaderNames.AUTHORIZATION).split(" ")[1])).split(Pattern.quote(":"));
                String username = credentials[0];
                String password = credentials[1];
                if (!(this.config.getUsername().equals(username) && this.config.getPassword().equals(password))) {
                    HttpUtils.sendStatus(ctx, this.request, HttpResponseStatus.UNAUTHORIZED);
                    return;
                }
                System.out.println(uri);
                if (uri.endsWith("/")) {
                    HttpUtils.sendStatus(ctx, this.request, HttpResponseStatus.BAD_REQUEST);
                    return;
                }
                if (file.exists()) {
                    file.delete();
                }
                file.getParentFile().mkdirs();
                file.createNewFile();

                if (this.outputStream == null) this.outputStream = new FileOutputStream(file);
                this.outputStream.write(content.array());

                if (last) try (final InputStream is = new FileInputStream(file)) {
                    this.outputStream.close();
                    HttpUtils.sendAndCleanupConnection(ctx, this.request, new DefaultHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.CREATED),
                            new DefaultHttpContent(Unpooled.copiedBuffer(is.readAllBytes())));
                }
                break;
            case "DELETE":
                break;
        }
    }
}
