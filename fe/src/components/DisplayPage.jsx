import { InputWithButton } from "./InputWithButton";
import { TypographyH2 } from "./Typographyh2";
import { Separator } from "@/components/ui/separator";

export default function DisplayPage() {
    return (
        <div>
            <div className="flex flex-row">
                <TypographyH2 />
                <InputWithButton />
            </div>
            <Separator className="my-4"/>
            <p className="text-muted-foreground text-xl mx-2">
            AI Overview
            </p>
            <img className="h-48 w-96 object-fill rounded-lg my-4 mx-2" src="https://i.pinimg.com/736x/33/4d/fb/334dfbec7aca0a73b1ed5a033be306b9.jpg" alt="image description" />

        </div>
    )
}